package com.themillhousegroup.play2.strava.services

import javax.inject.{ Inject, Singleton }

import com.themillhousegroup.play2.strava.models._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import StravaJson._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.themillhousegroup.play2.strava.services.traits.CachingStravaService
import play.api.Logger
import com.themillhousegroup.play2.strava.services.helpers.AuthBearer._

@Singleton
class StravaAthleteService @Inject() (val stravaAPI: StravaAPI, val cache: CacheApi)
    extends CachingStravaService[List[StravaSegmentEffort], Long] {

  val oneDay = Duration(24, "hours")

  val logger = Logger("StravaAthleteService")

  def getFullAthleteInfo(stravaAccessToken: String, athleteId: Long): Future[Option[StravaAthlete]] = {
    getWithBearerAuth(stravaAPI.athleteFinder(athleteId), stravaAccessToken).map { response =>
      if (response.status == 200) {
        Some(response.json.as[StravaAthlete])
      } else {
        None
      }
    }
  }

  def listFriendsFor(stravaAccessToken: String, athleteId: Long): Future[List[EssentialStravaAthlete]] = {
    val paginatedFriendsList = (page: Int) =>
      getWithBearerAuth(stravaAPI.allMyFriendsFinder(athleteId, page), stravaAccessToken).map { response =>
        logger.info(s"Friends response for athlete $athleteId, page $page: using token: $stravaAccessToken \n${response.json}")
        response.json.as[List[StravaAthleteSummary]]
      }

    StravaAPI.paginate(paginatedFriendsList)
  }

  val komCacheExpiry = Duration(3, "hours")

  def cacheNameFor(athleteId: Long) = s"komCache[${athleteId}]"

  def withKOMCacheFor(athleteId: Long, accessToken: String): Future[List[StravaSegmentEffort]] =
    withCacheFor(athleteId, accessToken)(listKOMsFor)

  private def listKOMsFor(stravaAccessToken: String, athleteId: Long): Future[List[StravaSegmentEffort]] = {

    val paginatedKOMList = (page: Int) =>
      getWithBearerAuth(stravaAPI.allMyKOMsFinder(athleteId, page), stravaAccessToken).map { response =>
        logger.info(s"KOMS response for athlete $athleteId, page $page: using token: $stravaAccessToken \n${response.json}")
        response.json.as[List[StravaSegmentEffort]]
      }

    StravaAPI.paginate(paginatedKOMList)
  }
}
