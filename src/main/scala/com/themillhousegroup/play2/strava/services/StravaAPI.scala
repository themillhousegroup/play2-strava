package com.themillhousegroup.play2.strava.services

import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ ExecutionContext, Future }
import play.api.libs.ws._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws
import play.api.{ Logger, libs }
import org.apache.commons.lang3.exception.ExceptionUtils
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }
import org.apache.commons.lang3.StringUtils

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import org.joda.time.{ DateTime, LocalDateTime }

object StravaAPI {

  val stravaV3BaseUrl = "https://www.strava.com/api/v3"

  object Athlete {
    val allMyActivitiesUrl = stravaV3BaseUrl + "/athlete/activities"
  }

  object Athletes {
    private val athletesEndpoint = stravaV3BaseUrl + "/athletes"
    def singleAthleteUrl(id: Long) = athletesEndpoint + s"/${id}"
    def allMyKOMsUrl(id: Long) = singleAthleteUrl(id) + "/koms"
    def allMyFriendsUrl(id: Long) = singleAthleteUrl(id) + "/friends"
  }

  val dateFormat = ISODateTimeFormat.dateTimeNoMillis()

  private val perPageParam = "per_page"
  private val pageParam = "page"
  val maxPageSize = 200

  private val logger = Logger("StravaAPI")

  /** Automatically loop around for more results in a paginated query */
  def depaginate[T](apiCall: Int => Future[Seq[T]], pageNumber: Int = 1): Future[Seq[T]] = {
    apiCall(pageNumber).flatMap { listOfThings =>
      if (listOfThings.size == StravaAPI.maxPageSize) {
        logger.info("API call returned MAX things; looping for more")
        depaginate(apiCall, pageNumber + 1).map { moreThings =>
          listOfThings ++ moreThings
        }
      } else {
        logger.info(s"Non MAX things: (${listOfThings.size} < ${StravaAPI.maxPageSize}) - will not page for more")
        Future.successful(listOfThings)
      }
    }
  }

  private val createActivityUrl = "https://www.strava.com/api/v3/activities"
  private def singleActivityUrl(id: Long) = stravaV3BaseUrl + s"/activities/${id}"
  private def clubActivityUrl(id: String) = stravaV3BaseUrl + s"/clubs/${id}/activities"
  private def segmentUrl(id: Long) = stravaV3BaseUrl + s"/segments/${id}"
  private def segmentEffortsUrl(id: Long) = stravaV3BaseUrl + s"/segments/${id}/all_efforts"
  private def activityStreamUrl(
    id: Long,
    streamType: String,
    resolution: Option[String],
    seriesType: Option[String]) = streamUrl(id, "activities", streamType, resolution, seriesType)

  private def segmentStreamUrl(id: Long,
    streamType: String,
    resolution: Option[String],
    seriesType: Option[String]) = streamUrl(id, "segments", streamType, resolution, seriesType)

  private def streamUrl(id: Long,
    baseType: String,
    streamType: String,
    resolution: Option[String],
    seriesType: Option[String]) = {
    val resolutionOption = resolution.map(r => s"?resolution=$r")
    val sType = seriesType.map(s => s"&series_type=$s").getOrElse("")

    val finalOptionsString = resolutionOption.map { r =>
      s"$r$sType"
    }.getOrElse("")
    stravaV3BaseUrl + s"/${baseType}/${id}/streams/${streamType}${finalOptionsString}"
  }

  private val segmentExploreUrl = stravaV3BaseUrl + "/segments/explore"

  private def activityPhotosUrl(activityId: Long, sizePx: Option[Int]) =
    stravaV3BaseUrl + s"""/activities/${activityId}/photos?photo_sources=true${sizePx.map(s => s"size=$s").getOrElse("")}"""
}

class StravaAPI @Inject() (val wsClient: WSClient) {
  import StravaAPI._

  //  def urlFinder(id:Option[String]) = id.fold(WS.url(listUrl))(i => WS.url(singleActivityUrl(i)))

  private def withPaginationQueryString(page: Int)(req: WSRequest): WSRequest = {
    req.withQueryString(pageParam -> page.toString, perPageParam -> maxPageSize.toString)
  }

  def allMyActivitiesFinder(page: Int): WSRequest = withPaginationQueryString(page) {
    wsClient.url(StravaAPI.Athlete.allMyActivitiesUrl)
  }

  val createActivityFinder = wsClient.url(createActivityUrl): WSRequest

  def allMyKOMsFinder(athleteId: Long, page: Int): WSRequest = withPaginationQueryString(page) {
    wsClient.url(StravaAPI.Athletes.allMyKOMsUrl(athleteId))
  }

  def athleteFinder(athleteId: Long): WSRequest = wsClient.url(StravaAPI.Athletes.singleAthleteUrl(athleteId))

  def allMyFriendsFinder(athleteId: Long, page: Int): WSRequest = withPaginationQueryString(page) {
    wsClient.url(StravaAPI.Athletes.allMyFriendsUrl(athleteId))
  }

  def singleActivityUrlFinder(activityId: Long): WSRequest = wsClient.url(singleActivityUrl(activityId))
  def clubActivityUrlFinder(clubId: String): WSRequest = wsClient.url(clubActivityUrl(clubId))
  def activityStreamUrlFinder(activityId: Long,
    streamType: String,
    resolution: Option[String],
    seriesType: Option[String]): WSRequest = {
    wsClient.url(activityStreamUrl(activityId, streamType, resolution, seriesType))
  }
  def segmentStreamUrlFinder(segmentId: Long,
    streamType: String,
    resolution: Option[String],
    seriesType: Option[String]): WSRequest = {
    wsClient.url(segmentStreamUrl(segmentId, streamType, resolution, seriesType))
  }

  def segmentUrlFinder(segmentId: Long): WSRequest = wsClient.url(segmentUrl(segmentId))
  def segmentEffortsUrlFinder(segmentId: Long,
    maybeAthleteId: Option[Long] = None,
    maybeStartFrom: Option[LocalDateTime] = None,
    maybeEndAt: Option[LocalDateTime] = None,
    page: Int): WSRequest = {
    logger.info(s"Requesting page $page of segment efforts for segment $segmentId ${maybeAthleteId.fold("")(aid => s"(athlete id $aid)")}")

    maybeStartFrom.fold(wsClient.url(segmentEffortsUrl(segmentId))) { startFrom =>

      val endAt = maybeEndAt.getOrElse(new LocalDateTime().plusDays(3))

      val params = Seq(
        "start_date_local" -> startFrom.toString(dateFormat),
        "end_date_local" -> endAt.toString(dateFormat)
      ) ++ maybeAthleteId.map(aid => "athlete_id" -> aid.toString)

      logger.info(s"Starting from ${startFrom.toString(dateFormat)}, ending at ${endAt.toString(dateFormat)}")
      wsClient.url(segmentEffortsUrl(segmentId)).withQueryString(params: _*)
    }.withQueryString(pageParam -> page.toString, perPageParam -> maxPageSize.toString)
  }

  def segmentExplorer(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): WSRequest = {
    val boundsString = s"$minLat,$minLon,$maxLat,$maxLon"
    wsClient.url(segmentExploreUrl).withQueryString("bounds" -> boundsString)
  }

  def activityPhotosUrlFinder(activityId: Long, sizePx: Option[Int]): WSRequest = wsClient.url(activityPhotosUrl(activityId, sizePx))
}