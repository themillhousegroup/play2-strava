package com.themillhousegroup.play2.strava.services

import javax.inject.{Inject, Singleton}

import com.themillhousegroup.arallon.{TimeInZone, TimeZone, UTC}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws._
import play.api.Play.current
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws
import play.api.{Logger, libs}
import org.apache.commons.lang3.exception.ExceptionUtils
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.apache.commons.lang3.StringUtils
import play.twirl.api.Html
import play.api.cache.CacheApi

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag


trait BearerAuthService {

  protected def withBearerAuth(finder: => WSRequest, accessToken:String):WSRequest = {
    finder.withHeaders("Authorization" -> s"Bearer ${accessToken}")
  }

  protected def getWithBearerAuth(finder: => WSRequest, accessToken:String):Future[WSResponse] = {
    withBearerAuth(finder, accessToken).get().map(updateUsage)
  }

  protected def updateUsage(response:WSResponse):WSResponse = response
}

object StravaAPI {
  val dateFormat = ISODateTimeFormat.dateTimeNoMillis()

  private val perPageParam = "per_page"
  private val pageParam = "page"
  val maxPageSize = 200

  private val logger = Logger("StravaAPI")

  def paginate[T](apiCall: Int => Future[List[T]], pageNumber:Int = 1):Future[List[T]] = {
    apiCall(pageNumber).flatMap { listOfThings =>
      if (listOfThings.size == StravaAPI.maxPageSize) {
        logger.info("API call returned MAX things; looping for more")
        paginate(apiCall,pageNumber +1).map { moreThings =>
          listOfThings ++ moreThings
        }
      } else {
        logger.info(s"Non MAX things: (${listOfThings.size} < ${StravaAPI.maxPageSize}) - will not page for more")
        Future.successful(listOfThings)
      }
    }
  }

  private val stravaV3BaseUrl = "https://www.strava.com/api/v3"
  private val allMyActivitiesUrl = stravaV3BaseUrl + "/athlete/activities"
  private def allMyKOMsUrl(id:Long) = stravaV3BaseUrl + s"/athletes/${id}/koms"
  private def singleAthleteUrl(id:Long) = stravaV3BaseUrl + s"/athletes/${id}"
  private def allMyFriendsUrl(id:Long) = stravaV3BaseUrl + s"/athletes/${id}/friends"
  private def singleActivityUrl(id:Long) = stravaV3BaseUrl + s"/activities/${id}"
  private def clubActivityUrl(id:String) = stravaV3BaseUrl + s"/clubs/${id}/activities"
  private def segmentUrl(id:Long) = stravaV3BaseUrl + s"/segments/${id}"
  private def segmentEffortsUrl(id:Long) = stravaV3BaseUrl + s"/segments/${id}/all_efforts"
  private def activityStreamUrl(id:Long, streamType:String) = stravaV3BaseUrl + s"/activities/${id}/streams/${streamType}"
  private val segmentExploreUrl = stravaV3BaseUrl + "/segments/explore"

}

class StravaAPI @Inject() (val wsClient:WSClient) {
  import StravaAPI._

//  def urlFinder(id:Option[String]) = id.fold(WS.url(listUrl))(i => WS.url(singleActivityUrl(i)))

  def withPaginationQueryString(page:Int)(req:WSRequest) = {
     req.withQueryString(pageParam -> page.toString, perPageParam -> maxPageSize.toString)
  }
		
  def allMyActivitiesFinder = wsClient.url(allMyActivitiesUrl)

  def allMyKOMsFinder(athleteId:Long, page:Int) = withPaginationQueryString(page) {
    wsClient.url(allMyKOMsUrl(athleteId))
  }

  def athleteFinder(athleteId:Long) = wsClient.url(singleAthleteUrl(athleteId))

  def allMyFriendsFinder(athleteId:Long, page:Int) = withPaginationQueryString(page) {
    wsClient.url(allMyFriendsUrl(athleteId))
  }


  def singleActivityUrlFinder(activityId:Long) = wsClient.url(singleActivityUrl(activityId))
  def clubActivityUrlFinder(clubId:String) = wsClient.url(clubActivityUrl(clubId))
  def activityStreamUrlFinder(activityId:Long,  streamType:String) = wsClient.url(activityStreamUrl(activityId, streamType))
  def segmentUrlFinder(segmentId:Long) = wsClient.url(segmentUrl(segmentId))
  def segmentEffortsUrlFinder[TZ <: TimeZone : ClassTag](segmentId:Long,
                              maybeAthleteId:Option[Long] = None,
                              maybeStartFrom:Option[TimeInZone[TZ]] = None,
                              maybeEndAt:Option[TimeInZone[TZ]] = None,
                              page:Int) = {
    logger.info(s"Requesting page $page of segment efforts for segment $segmentId ${maybeAthleteId.fold("")(aid => s"(athlete id $aid)")}")

    maybeStartFrom.fold(wsClient.url(segmentEffortsUrl(segmentId))) { startFrom =>

      val endAt = maybeEndAt.getOrElse(TimeInZone[TZ].transform(_.plusDays(3)))

      val params = Seq(
        "start_date_local" -> startFrom.asLocalDateTime.toString(dateFormat),
        "end_date_local" -> endAt.asLocalDateTime.toString(dateFormat)
      ) ++ maybeAthleteId.map(aid => "athlete_id" -> aid.toString)

      logger.info(s"Starting from ${startFrom.asLocalDateTime.toString(dateFormat)}, ending at ${endAt.asLocalDateTime.toString(dateFormat)}")
      wsClient.url(segmentEffortsUrl(segmentId)).withQueryString(params:_*)
    }.withQueryString(pageParam -> page.toString, perPageParam -> maxPageSize.toString)
  }

  def segmentExplorer(minLat:Double, minLon:Double, maxLat:Double, maxLon:Double) = {
    val boundsString = s"$minLat,$minLon,$maxLat,$maxLon"
    wsClient.url(segmentExploreUrl).withQueryString("bounds" -> boundsString)
  }

}

trait CachingStravaService[T, ID] {
  val cache:CacheApi
  val logger:Logger

  protected val cacheExpiry = Duration(3, "hours")

  protected def cacheNameFor(entityId:ID):String

  protected def withCacheFor[T:ClassTag](entityId:ID, accessToken:String)(fetcher: (String, ID) => Future[T]):Future[T] = {
    val cacheName = cacheNameFor(entityId)
    val maybeResult = cache.get[T](cacheName)

    maybeResult.fold {
      logger.warn(s"Cache miss for $cacheName")
      fetcher(accessToken, entityId).map { entity =>
        cache.set(cacheName, entity, cacheExpiry)
        entity
      }
    } { hit =>
      logger.debug(s"Cache hit for $cacheName")
      Future.successful(hit)
    }
  }
}
