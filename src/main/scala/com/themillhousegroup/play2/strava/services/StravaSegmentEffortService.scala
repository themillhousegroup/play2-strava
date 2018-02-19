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
class StravaSegmentEffortService @Inject() (val stravaAPI: StravaAPI, val requester: StandardRequestResponseHelper) {

  val logger = Logger("StravaSegmentEffortService")

  def findSegmentEffort(stravaAccessToken: String, segmentEffortId: Long): Future[Option[StravaSegmentEffort]] = {
    import StravaSegmentJson.stravaSegmentEffortFormat
    requester.optional(
      stravaAccessToken,
      stravaAPI.segmentEffortUrlFinder(segmentEffortId)
    )
  }

  def getSegmentEffort(stravaAccessToken: String, segmentEffortId: Long): Future[StravaSegmentEffort] = {
    import StravaSegmentJson.stravaSegmentEffortFormat
    requester(
      stravaAccessToken,
      stravaAPI.segmentEffortUrlFinder(segmentEffortId)
    )
  }
}
