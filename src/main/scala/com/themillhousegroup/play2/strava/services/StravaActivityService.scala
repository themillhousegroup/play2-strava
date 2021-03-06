package com.themillhousegroup.play2.strava.services

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.json.Format

import scala.concurrent.Future
import com.themillhousegroup.play2.strava.models._
import com.themillhousegroup.play2.strava.services.helpers.StandardRequestResponseHelper
import org.joda.time.DateTime

@Singleton
class StravaActivityService @Inject()(val stravaAPI:StravaAPI, requester:StandardRequestResponseHelper, cacheApi:CacheApi) {

  val logger = Logger(classOf[StravaActivityService])

  /**
    * https://strava.github.io/api/v3/activities/#get-activities
    *
    * EXAMPLE REQUEST
    * $ curl -G https://www.strava.com/api/v3/athlete/activities \
    * -H "Authorization: Bearer 83ebeabdec09f6670863766f792ead24d61fe3f9"
    * -d per_page=1
    */
  def listActivitiesFor(stravaAccessToken:String, maybePage:Option[Int] = None):Future[Seq[StravaActivitySummary]] = {
    import StravaActivitySummaryJson._
    requester.seq(
      stravaAccessToken,
      stravaAPI.allMyActivitiesFinder(maybePage.getOrElse(1))
    )
  }

  /** Supply the activity type you want to deserialize to, and the Format to do it with */
  def findSingleActivity[SA <: EssentialStravaActivity](stravaAccessToken:String, id:Long)(fmt:Format[SA]):Future[Option[SA]] = {
    requester.optional(
      stravaAccessToken,
      stravaAPI.singleActivityUrlFinder(id)
    )(fmt)
  }

  /** Supply the activity type you want to deserialize to, and the Format to do it with */
  def getSingleActivity[SA <: EssentialStravaActivity](stravaAccessToken:String, id:Long)(fmt:Format[SA]):Future[SA] = {
    requester(
      stravaAccessToken,
      stravaAPI.singleActivityUrlFinder(id)
    )(fmt)
  }

  /** Supply None as the 'resolution' param to get "ALL".
    * Valid values: "low", "medium", "high"
    *
    * If specifying a resolution, the series type can also be specified.
    * Either "time" or "distance" is valid.
    *
    * Possible stream types:
    * time:	integer seconds
    * latlng:	floats [latitude, longitude]
    * distance:	float meters
    * altitude:	float meters
    * velocity_smooth:	float meters per second
    * heartrate:	integer BPM
    * cadence:	integer RPM
    * watts:	integer watts
    * temp:	integer degrees Celsius
    * moving:	boolean
    * grade_smooth:
    *
    */
  def getActivityStream(stravaAccessToken:String, id:Long, streamType:String, resolution:Option[String], seriesType:Option[String]):Future[Seq[StravaStreamObject]] = {
    import StravaStreamJson._
    requester.seq(
      stravaAccessToken,
      stravaAPI.activityStreamUrlFinder(id, streamType, resolution, seriesType)
    )
  }

  def listActivitiesAfter(stravaAccessToken:String, time:DateTime):Future[Seq[StravaActivitySummary]] = {
    val afterInSecondsFromEpoch = time.getMillis / 1000
    logger.info(s"Requesting Strava entries for $stravaAccessToken AFTER $afterInSecondsFromEpoch")

    import StravaActivitySummaryJson._
    val paginatedActivityList = (page:Int) =>
      requester.seq(
        stravaAccessToken,
        stravaAPI.allMyActivitiesFinder(page).withQueryString("after" -> afterInSecondsFromEpoch.toString)
      )

    StravaAPI.depaginate(paginatedActivityList)
  }

  def listActivitiesBefore(stravaAccessToken:String, time:DateTime):Future[Seq[StravaActivitySummary]] = {
    val beforeInSecondsFromEpoch = time.getMillis / 1000
    logger.info(s"Requesting Strava entries for $stravaAccessToken BEFORE $beforeInSecondsFromEpoch")

    import StravaActivitySummaryJson._
    val paginatedActivityList = (page:Int) =>
      requester.seq(
        stravaAccessToken,
        stravaAPI.allMyActivitiesFinder(page).withQueryString("before" -> beforeInSecondsFromEpoch.toString)
      )

    StravaAPI.depaginate(paginatedActivityList)
  }

  def listActivitiesBetween(stravaAccessToken:String, startTime:DateTime, endTime: DateTime):Future[Seq[StravaActivitySummary]] = {
    val afterInSecondsFromEpoch = startTime.getMillis / 1000
    val beforeInSecondsFromEpoch = endTime.getMillis / 1000
    logger.info(s"Requesting Strava entries for $stravaAccessToken BETWEEN $afterInSecondsFromEpoch and $beforeInSecondsFromEpoch")

    import StravaActivitySummaryJson._
    val paginatedActivityList = (page:Int) =>
      requester.seq(
        stravaAccessToken,
        stravaAPI.allMyActivitiesFinder(page).withQueryString("after" -> afterInSecondsFromEpoch.toString, "before" -> beforeInSecondsFromEpoch.toString)
      )

    StravaAPI.depaginate(paginatedActivityList)
  }

}
