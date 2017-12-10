package com.themillhousegroup.play2.strava.test

import com.themillhousegroup.play2.strava.services.helpers.AuthBearer
import org.mockito.Matchers
import org.specs2.mock.Mockito
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }

import scala.concurrent.Future

trait StandardMocks extends Mockito {

  def buildMockRequest: WSRequest = {
    val req = mock[WSRequest]
    req.withHeaders(any[(String, String)]) returns req
    req
  }

  val notFoundRequest = buildMockRequest
  val badJsonRequest = buildMockRequest

  def buildMockResponse(status: Int, jsonBody: Option[JsValue] = None): WSResponse = {
    val r = mock[WSResponse]
    r.status returns status
    r.header(anyString) returns None
    jsonBody.foreach(body => r.json returns body)
    r
  }

  val notFoundResponse = buildMockResponse(404)
  val badJsonResponse = buildMockResponse(200, Some(Json.obj("foo" -> "bar")))

  val mockAuthBearer = mock[AuthBearer]
  def requestWillReturn(request: WSRequest, response: WSResponse): WSRequest = {
    mockAuthBearer.getWithBearerAuth(Matchers.eq(request), anyString) returns Future.successful(response)

    request
  }

  def mockRequestThatReturns(response: WSResponse): WSRequest = {

    val request = buildMockRequest

    mockAuthBearer.getWithBearerAuth(Matchers.eq(request), anyString) returns Future.successful(response)

    request
  }

  requestWillReturn(notFoundRequest, notFoundResponse)
  requestWillReturn(badJsonRequest, badJsonResponse)
}
