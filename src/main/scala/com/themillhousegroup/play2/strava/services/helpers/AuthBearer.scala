package com.themillhousegroup.play2.strava.services.helpers

import com.themillhousegroup.play2.strava.services.traits.UsageMonitoring

import scala.concurrent.Future
import play.api.libs.ws._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object AuthBearer extends UsageMonitoring {

  protected val logger = Logger("AuthBearer")

  def withBearerAuth(finder: => WSRequest, accessToken: String): WSRequest = {
    finder.withHeaders("Authorization" -> s"Bearer ${accessToken}")
  }

  def getWithBearerAuth(finder: => WSRequest, accessToken: String): Future[WSResponse] = {
    withBearerAuth(finder, accessToken).get().map(updateUsage)
  }

}