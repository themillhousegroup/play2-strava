package com.themillhousegroup.play2.strava.services.helpers

import scala.concurrent.Future
import play.api.libs.ws._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object AuthBearer {

  private val logger = Logger("AuthBearer")

  def withBearerAuth(finder: => WSRequest, accessToken: String): WSRequest = {
    finder.withHeaders("Authorization" -> s"Bearer ${accessToken}")
  }

  def getWithBearerAuth(finder: => WSRequest, accessToken: String): Future[WSResponse] = {
    withBearerAuth(finder, accessToken).get().map(updateUsage)
  }

  //protected def updateUsage(response: WSResponse): WSResponse = response

  private def updateUsage(response: WSResponse): WSResponse = {
    response.header("X-Ratelimit-Usage").map { usage =>
      val parts = usage.split(",")
      fifteenMinuteUsage = parts.headOption.map(_.toInt)
      logger.info(s"Last 15 minute usage: $fifteenMinuteUsage")
      dailyUsage = parts.lastOption.map(_.toInt)
    }

    response.header("X-RateLimit-Limit").map { limits =>
      val parts = limits.split(",")
      fifteenMinuteRequestLimit = parts.headOption.map(_.toInt).getOrElse(0)
      logger.info(s"Last 15 minute usage: $fifteenMinuteUsage")
      dailyRequestLimit = parts.lastOption.map(_.toInt).getOrElse(0)
    }

    response
  }

  var fifteenMinuteUsage: Option[Int] = None
  var dailyUsage: Option[Int] = None

  var fifteenMinuteRequestLimit = 600
  var dailyRequestLimit = 30000
}