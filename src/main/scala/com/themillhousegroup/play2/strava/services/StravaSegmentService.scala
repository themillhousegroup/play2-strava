package com.themillhousegroup.play2.strava.services

import javax.inject.{ Inject, Singleton }

import com.themillhousegroup.play2.strava.models.{ StravaSegment, StravaSegmentEffort, StravaStreamObject }
import com.themillhousegroup.play2.strava.services.traits.CachingStravaService
import com.themillhousegroup.play2.strava.models._
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.themillhousegroup.play2.strava.services.helpers.AuthBearer._
import org.joda.time.LocalDateTime
import StravaJson._

import scala.concurrent.Future

@Singleton
class StravaSegmentService @Inject() (val stravaAPI: StravaAPI, val cache: CacheApi)
    extends CachingStravaService[Long] {

  val logger = Logger("StravaSegmentService")

  def cacheNameFor(segmentId: Long) = s"segmentCache[${segmentId}]"

  def withSegmentCacheFor(segmentId: Long, accessToken: String): Future[StravaSegment] =
    withCacheFor(segmentId, accessToken)(getSegment)

  def getSegment(stravaAccessToken: String, segmentId: Long): Future[StravaSegment] = {

    getWithBearerAuth(stravaAPI.segmentUrlFinder(segmentId), stravaAccessToken).map { response =>
      response.json.as[StravaSegment]
    }
  }

  def getSegmentEfforts(stravaAccessToken: String,
    segmentId: Long,
    maybeAthleteId: Option[Long] = None,
    maybeStartFrom: Option[LocalDateTime] = None,
    maybeEndAt: Option[LocalDateTime] = None): Future[Seq[StravaSegmentEffort]] = {

    val paginatedSegmentEffort = (page: Int) =>
      getWithBearerAuth(stravaAPI.segmentEffortsUrlFinder(segmentId, maybeAthleteId, maybeStartFrom, maybeEndAt, page), stravaAccessToken).map { response =>
        response.json.validate[List[StravaSegmentEffort]].getOrElse {
          logger.warn(s"Couldn't parse ${response.json}")
          Nil
        }
      }

    StravaAPI.paginate(paginatedSegmentEffort)
  }

  /**
   * Stream types:
   * time:	integer seconds
   * latlng:	floats [latitude, longitude]
   * distance:	float meters
   * altitude:	float meters
   */

  def getSegmentStream(stravaAccessToken: String, segmentId: Long, streamType: String, resolution: Option[String], seriesType: Option[String]): Future[Seq[StravaStreamObject]] = {
    getWithBearerAuth(stravaAPI.segmentStreamUrlFinder(segmentId, streamType, resolution, seriesType), stravaAccessToken).map { response =>
      logger.debug(s"Dumping segment $segmentId stream response: \n$response")
      response.status match {
        case 200 => response.json.as[Seq[StravaStreamObject]]
        case _ => Nil // Or throw exception?
      }
    }
  }

  /** Finds the unique set of activity IDs that hit the given segments between `since` and `until` */
  def findActivityIdsInvolvingSegments(stravaAccessToken: String,
    segmentIds: Set[Long],
    since: LocalDateTime,
    until: LocalDateTime): Future[Set[Long]] = {
    val fActIds: Set[Future[Seq[Long]]] = segmentIds.map { segmentId =>
      getSegmentEfforts(stravaAccessToken, segmentId, None, Some(since), Some(until)).map { segmentEfforts =>
        logger.info(s"Found ${segmentEfforts.size} efforts on segment $segmentId")
        segmentEfforts.map(_.activity.id)
      }
    }

    val f: Future[Set[Seq[Long]]] = Future.sequence(fActIds)
    f.map { ll =>
      val flattened = ll.flatten
      logger.info(s"Flattened to ${flattened.size} activities")
      flattened
    }
  }

  def exploreWithBounds(stravaAccessToken: String, minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): Future[Set[StravaSegment]] = {

    getWithBearerAuth(stravaAPI.segmentExplorer(minLat, minLon, maxLat, maxLon), stravaAccessToken).map { response =>
      val segmentsArray = (response.json \ "segments")

      segmentsArray.toOption.fold {
        logger.warn(s"Couldn't find expected 'segments' array in $response.json")
        Set.empty[StravaSegment]
      } { segs =>
        segs.validate[Set[StravaSegment]].fold(err => {
          logger.warn(s"Couldn't parse ${segs}: $err")
          Set.empty[StravaSegment]
        }, segs => {
          logger.info(s"OK: $segs")
          segs
        }
        )
      }

    }
  }
}
