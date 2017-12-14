package com.themillhousegroup.play2.strava.services

import javax.inject.{ Inject, Singleton }

import com.themillhousegroup.play2.strava.models.{ StravaSegment, StravaSegmentEffort, StravaStreamObject }
import com.themillhousegroup.play2.strava.models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.LocalDateTime
import com.themillhousegroup.play2.strava.services.helpers.StandardRequestResponseHelper
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

@Singleton
class StravaSegmentService @Inject() (val stravaAPI: StravaAPI, val requester: StandardRequestResponseHelper) {

  val logger = Logger("StravaSegmentService")

  def findSegment(stravaAccessToken: String, segmentId: Long): Future[Option[StravaSegment]] = {
    import StravaSegmentJson.stravaSegmentFormat
    requester.optional(
      stravaAccessToken,
      stravaAPI.segmentUrlFinder(segmentId)
    )
  }

  def getSegment(stravaAccessToken: String, segmentId: Long): Future[StravaSegment] = {
    import StravaSegmentJson.stravaSegmentFormat
    requester(
      stravaAccessToken,
      stravaAPI.segmentUrlFinder(segmentId)
    )
  }

  def getSegmentEfforts(stravaAccessToken: String,
    segmentId: Long,
    maybeAthleteId: Option[Long] = None,
    maybeStartFrom: Option[LocalDateTime] = None,
    maybeEndAt: Option[LocalDateTime] = None): Future[Seq[StravaSegmentEffort]] = {

    import StravaSegmentJson.stravaSegmentEffortFormat

    val paginatedSegmentEffort = (page: Int) =>
      requester.seq(
        stravaAccessToken,
        stravaAPI.segmentEffortsUrlFinder(segmentId, maybeAthleteId, maybeStartFrom, maybeEndAt, page)
      )

    StravaAPI.depaginate(paginatedSegmentEffort)
  }

  /**
   * Stream types:
   * time:	integer seconds
   * latlng:	floats [latitude, longitude]
   * distance:	float meters
   * altitude:	float meters
   */

  def getSegmentStream(stravaAccessToken: String, segmentId: Long, streamType: String, resolution: Option[String], seriesType: Option[String]): Future[Seq[StravaStreamObject]] = {
    import StravaStreamJson.stravaStreamObjectFormat
    requester.seq(
      stravaAccessToken,
      stravaAPI.segmentStreamUrlFinder(segmentId, streamType, resolution, seriesType)
    )
  }

  def exploreWithBounds(stravaAccessToken: String, minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): Future[Set[StravaSegment]] = {

    import StravaSegmentJson.stravaSegmentFormat

    val segmentArrayConverter: (WSResponse) => Seq[StravaSegment] = { response =>
      val segmentsArray = (response.json \ "segments")
      segmentsArray.toOption.fold {
        Seq[StravaSegment]()
      } { segs =>
        segs.validate[Seq[StravaSegment]].getOrElse(Seq[StravaSegment]())
      }
    }

    requester.seq(
      stravaAccessToken,
      stravaAPI.segmentExplorer(minLat, minLon, maxLat, maxLon),
      segmentArrayConverter
    ).map { segmentSeq =>
        segmentSeq.toSet
      }

  }
}
