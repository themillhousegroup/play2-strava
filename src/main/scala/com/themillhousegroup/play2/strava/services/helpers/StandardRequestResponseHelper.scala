package com.themillhousegroup.play2.strava.services.helpers

import play.api.libs.json.Reads
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.themillhousegroup.play2.strava.services.helpers.AuthBearer._

import scala.concurrent.Future

class StandardRequestResponseHelper {

  private def asyncWrap[R](syncResponseHandler: WSResponse => R): WSResponse => Future[R] = { resp =>
    Future(syncResponseHandler(resp))
  }

  private def defaultResponseHandler[T](implicit rds: Reads[T]): WSResponse => Option[T] = { response =>
    if (response.status == 200) {
      response.json.asOpt[T]
    } else {
      None
    }
  }
  private def defaultSeqResponseHandler[T](implicit rds: Reads[T]): WSResponse => Seq[T] = { response =>
    if (response.status == 200) {
      response.json.as[Seq[T]]
    } else {
      Nil
    }
  }

  def apply[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Option[T]] = {
    apply(
      stravaAccessToken,
      request,
      defaultResponseHandler(rds)
    )
  }

  def apply[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Option[T])(implicit rds: Reads[T]): Future[Option[T]] = {

    async(
      stravaAccessToken,
      request,
      asyncWrap(responseHandler)
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Option[T]] = {
    async(
      stravaAccessToken,
      request,
      asyncWrap(defaultResponseHandler(rds))
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[Option[T]])(implicit rds: Reads[T]): Future[Option[T]] = {
    getWithBearerAuth(request, stravaAccessToken).flatMap(responseHandler)
  }

  //////////////////////////////////////////////////////////////////////////////////
  // Helpers for dealing with Sequences of domain objects
  //////////////////////////////////////////////////////////////////////////////////

  def seq[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Seq[T]] = {
    seq(
      stravaAccessToken,
      request,
      defaultSeqResponseHandler(rds)
    )
  }

  def seq[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Seq[T])(implicit rds: Reads[T]): Future[Seq[T]] = {
    asyncSeq(
      stravaAccessToken,
      request,
      asyncWrap(responseHandler)
    )
  }

  def asyncSeq[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Seq[T]] = {
    asyncSeq(
      stravaAccessToken,
      request,
      (resp) => Future(defaultSeqResponseHandler(rds)(resp))
    )
  }

  def asyncSeq[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[Seq[T]])(implicit rds: Reads[T]): Future[Seq[T]] = {
    getWithBearerAuth(request, stravaAccessToken).flatMap(responseHandler)
  }
}
