package com.themillhousegroup.play2.strava.services.helpers

import play.api.libs.json.Reads
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.themillhousegroup.play2.strava.services.helpers.AuthBearer._

import scala.concurrent.Future

class StandardRequestResponseHelper {

  private def asyncWrap[T](syncResponseHandler: WSResponse => T)(resp: WSResponse) = Future(syncResponseHandler(resp))
  private def defaultResponseHandler[T](implicit rds: Reads[T]): WSResponse => T = { response =>
    response.json.as[T]
  }
  private def defaultSeqResponseHandler[T](implicit rds: Reads[T]): WSResponse => Seq[T] = { response =>
    response.json.as[Seq[T]]
  }

  def apply[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[T] = {
    apply(
      stravaAccessToken,
      request,
      defaultResponseHandler(rds)
    )
  }

  def seq[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Seq[T]] = {
    seq(
      stravaAccessToken,
      request,
      defaultSeqResponseHandler(rds)
    )
  }

  def apply[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => T)(implicit rds: Reads[T]): Future[T] = {

    async(
      stravaAccessToken,
      request,
      asyncWrap(responseHandler)
    )
  }

  def seq[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Seq[T])(implicit rds: Reads[T]): Future[Seq[T]] = {
    apply(
      stravaAccessToken,
      request,
      responseHandler
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[T] = {
    async(
      stravaAccessToken,
      request,
      asyncWrap(defaultResponseHandler(rds))
    )
  }

  def asyncSeq[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Seq[T]] = {
    async(
      stravaAccessToken,
      request,
      (resp) => Future(defaultSeqResponseHandler(rds)(resp))
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[T])(implicit rds: Reads[T]): Future[T] = {
    getWithBearerAuth(request, stravaAccessToken).flatMap(responseHandler)
  }

  def asyncSeq[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[Seq[T]])(implicit rds: Reads[T]): Future[Seq[T]] = {
    getWithBearerAuth(request, stravaAccessToken).flatMap(responseHandler)
  }
}
