package com.themillhousegroup.play2.strava.services

import javax.inject.{ Inject, Singleton }

import com.themillhousegroup.play2.strava.models._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.themillhousegroup.play2.strava.services.traits.CachingStravaService
import play.api.Logger
import com.themillhousegroup.play2.strava.services.helpers.StandardRequestResponseHelper

@Singleton
class StravaAthleteService @Inject() (val stravaAPI: StravaAPI, val cache: CacheApi, val requester: StandardRequestResponseHelper)
    extends CachingStravaService[Long] {

  val logger = Logger("StravaAthleteService")

  def getFullAthleteInfo(stravaAccessToken: String, athleteId: Long): Future[Option[StravaAthlete]] = {
    import StravaAthleteJson._

    requester.optional(
      stravaAccessToken,
      stravaAPI.athleteFinder(athleteId)
    )
  }

  def listFriendsFor(stravaAccessToken: String, athleteId: Long): Future[Seq[StravaAthlete]] = {
    import StravaAthleteJson._

    val paginatedFriendsList = (page: Int) =>
      requester.seq(
        stravaAccessToken,
        stravaAPI.allMyFriendsFinder(athleteId, page)
      )

    StravaAPI.depaginate(paginatedFriendsList)
  }

  val komCacheExpiry = Duration(3, "hours")

  def cacheNameFor(athleteId: Long) = s"komCache[${athleteId}]"

  def withKOMCacheFor(athleteId: Long, accessToken: String): Future[Seq[StravaSegmentEffort]] =
    withCacheFor(athleteId, accessToken)(listKOMsFor)

  private def listKOMsFor(stravaAccessToken: String, athleteId: Long): Future[Seq[StravaSegmentEffort]] = {
    import StravaSegmentJson.stravaSegmentEffortFormat

    val paginatedKOMList = (page: Int) =>
      requester.seq(
        stravaAccessToken,
        stravaAPI.allMyKOMsFinder(athleteId, page)
      )

    StravaAPI.depaginate(paginatedKOMList)
  }
}
