package com.themillhousegroup.play2.strava.services

import javax.inject.{Inject, Singleton}

import com.themillhousegroup.arallon.TimeInZone

import org.apache.commons.lang3.exception.ExceptionUtils
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Format
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import com.themillhousegroup.play2.strava.models._
import StravaJson._

import com.themillhousegroup.play2.strava.services.helpers.AuthBearer._

@Singleton
class  StravaActivityService @Inject()(val stravaAPI:StravaAPI, cache:CacheApi) {


  var fifteenMinuteUsage:Option[Int] = None
  var dailyUsage:Option[Int] = None

  val fifteenMinuteRequestLimit=600
  val dailyRequestLimit=30000

  val oneDay = Duration(24, "hours")

  val logger = Logger("StravaActivityService")

  /**
    * https://strava.github.io/api/v3/activities/#get-activities
    *
    * EXAMPLE REQUEST
    * $ curl -G https://www.strava.com/api/v3/athlete/activities \
    * -H "Authorization: Bearer 83ebeabdec09f6670863766f792ead24d61fe3f9"
    * -d per_page=1
    */
  def listActivitiesFor(stravaAccessToken:String, maybePage:Option[Int] = None):Future[Seq[StravaActivity]] = {
    getWithBearerAuth(stravaAPI.allMyActivitiesFinder(maybePage.getOrElse(1)), stravaAccessToken).map { response =>
      response.json.as[Seq[StravaActivity]].filter(_.`type` == "Ride")
    }
  }

  /** Supply the activity type you want to deserialize to, and the Format to do it with */
  def getSingleActivity[SA <: EssentialStravaActivity](stravaAccessToken:String, id:Long)(fmt:Format[SA]):Future[SA] = {
    getWithBearerAuth(stravaAPI.singleActivityUrlFinder(id), stravaAccessToken).map { response =>
      response.json.as[SA](fmt)
    }
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
    getWithBearerAuth(stravaAPI.activityStreamUrlFinder(id, streamType, resolution, seriesType), stravaAccessToken).map { response =>

      val respJson = response.json
      logger.debug(s"Response for $streamType is: $respJson")
      respJson.as[Seq[StravaStreamObject]]
    }
  }

  def listActivitiesAfter(stravaAccessToken:String, time:TimeInZone[_]):Future[Seq[StravaActivitySummary]] = {
    val afterInSecondsFromEpoch = time.utcMillis / 1000
    logger.info(s"Requesting Strava entries for $stravaAccessToken AFTER $afterInSecondsFromEpoch")

    val paginatedActivityList = (page:Int) =>

      withBearerAuth(stravaAPI.allMyActivitiesFinder(page), stravaAccessToken)
        .withQueryString("after" -> afterInSecondsFromEpoch.toString).get().map { response =>
        response.json.as[Seq[StravaActivitySummary]]
      }

    StravaAPI.paginate(paginatedActivityList).map { unfiltered =>
      try {
        val filtered = unfiltered.filter(_.`type` == "Ride")
        logger.info(s"Token $stravaAccessToken has ${unfiltered.size} Strava activities, filtered to ${filtered.size}")

        filtered
      } catch {
        case t:Throwable => logger.error(s"Activity parsing fail: ${ExceptionUtils.getStackTrace(t)}"); Nil
      }
    }
  }

  def listActivityPhotos(stravaAccessToken:String, activityId:Long, sizePx:Option[Int]):Future[Seq[EssentialStravaActivityPhoto]] = {
    getWithBearerAuth(stravaAPI.activityPhotosUrlFinder(activityId, sizePx), stravaAccessToken).map { response =>
      response.json.as[Seq[EssentialStravaActivityPhoto]].sorted
    }
  }
}
