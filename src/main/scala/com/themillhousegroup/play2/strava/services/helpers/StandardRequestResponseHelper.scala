package com.themillhousegroup.play2.strava.services.helpers

import play.api.libs.json.Reads
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.themillhousegroup.play2.strava.services.helpers.AuthBearer._

import scala.concurrent.Future

class StandardRequestResponseHelper {

  private def asyncResponseHandler[T](syncResponseHandler: WSResponse => T)(resp: WSResponse) = Future(syncResponseHandler(resp))
  private def defaultResponseHandler[T](rds: Reads[T])(response: WSResponse): T = response.json.as[T](rds)

  def apply[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[T] = {
    apply(
      stravaAccessToken,
      request,
      defaultResponseHandler(rds)
    )
  }

  def apply[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => T)(implicit rds: Reads[T]): Future[T] = {

    async(
      stravaAccessToken,
      request,
      asyncResponseHandler(responseHandler)
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[T] = {
    async(
      stravaAccessToken,
      request,
      asyncResponseHandler(defaultResponseHandler(rds))
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[T])(implicit rds: Reads[T]): Future[T] = {
    getWithBearerAuth(request, stravaAccessToken).map { response =>
      response.json.as[T]
    }
  }
}
