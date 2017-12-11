package com.themillhousegroup.play2.strava.services.helpers

import javax.inject.Inject

import play.api.libs.json._
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger

import scala.concurrent.Future

class StandardRequestResponseHelper @Inject() (val authBearer: AuthBearer) {

  private val logger = Logger(classOf[StandardRequestResponseHelper])

  private def asyncWrap[R](syncResponseHandler: WSResponse => R): WSResponse => Future[R] = { resp =>
    Future(syncResponseHandler(resp))
  }

  private def innerResponseHandler[T, R](validator: JsValue => JsResult[R])(onFail: => R)(implicit rds: Reads[T]): WSResponse => R = { response =>
    if (response.status == 200) {

      logger.trace(s"Attempting conversion of:\n${response.json}")

      val validationResult = validator(response.json)

      validationResult.getOrElse {
        val message = s"Conversion of ${response.json} was unsuccessful; ${validationResult.toString}; returning the 'fail' response"
        logger.warn(message)
        onFail
      }
    } else {
      logger.warn(s"Encountered response code ${response.status} - message: ${response.body}")
      onFail
    }
  }

  private def defaultOptionalResponseHandler[T](implicit rds: Reads[T]): WSResponse => Option[T] = innerResponseHandler[T, Option[T]](_.validateOpt[T])(None)(rds)

  private def defaultSeqResponseHandler[T](implicit rds: Reads[T]): WSResponse => Seq[T] = innerResponseHandler[T, Seq[T]](_.validate[Seq[T]])(Nil)(rds)

  def apply[T](stravaAccessToken: String,
    request: WSRequest)(implicit rds: Reads[T]): Future[Option[T]] = {

    apply(
      stravaAccessToken,
      request,
      defaultOptionalResponseHandler(rds)
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
      asyncWrap(defaultOptionalResponseHandler(rds))
    )
  }

  def async[T](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[Option[T]])(implicit rds: Reads[T]): Future[Option[T]] = {

    makeRequest(stravaAccessToken, request, responseHandler)
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

    makeRequest(stravaAccessToken, request, responseHandler)
  }

  private def makeRequest[T, R](stravaAccessToken: String,
    request: WSRequest,
    responseHandler: WSResponse => Future[R])(implicit rds: Reads[T]): Future[R] = {
    authBearer.getWithBearerAuth(request, stravaAccessToken).flatMap(responseHandler)
  }
}
