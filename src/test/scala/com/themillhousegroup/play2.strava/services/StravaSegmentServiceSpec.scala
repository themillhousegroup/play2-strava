package com.themillhousegroup.play2.strava.services

import com.themillhousegroup.play2.strava.services.helpers.{ AuthBearer, StandardRequestResponseHelper }
import com.themillhousegroup.play2.strava.test.TestFixtures
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }

import scala.concurrent.Future

class StravaSegmentServiceSpec extends Specification with Mockito with TestFixtures {

  val TOKEN = "Token"

  val now = new DateTime()

  val notFoundResponse = mock[WSResponse]
  notFoundResponse.status returns 404
  notFoundResponse.header(anyString) returns None

  val notFoundRequest = mock[WSRequest]
  notFoundRequest.withHeaders(any[(String, String)]) returns notFoundRequest
  notFoundRequest.get returns Future.successful(notFoundResponse)

  val badJsonResponse = mock[WSResponse]
  badJsonResponse.status returns 200
  badJsonResponse.header(anyString) returns None
  badJsonResponse.json returns Json.obj("foo" -> "bar")

  val badJsonRequest = mock[WSRequest]
  badJsonRequest.withHeaders(any[(String, String)]) returns badJsonRequest
  badJsonRequest.get returns Future.successful(badJsonResponse)

  val mockAuthBearer = mock[AuthBearer]
  mockAuthBearer.getWithBearerAuth(any[WSRequest], anyString) returns Future.successful(notFoundResponse)
  val mockStravaAPI = mock[StravaAPI]

  val requester = new StandardRequestResponseHelper(mockAuthBearer)

  val segmentService = new StravaSegmentService(mockStravaAPI, requester)

  "StravaSegmentService - getSegment" should {

    val validSegmentJson = Json.obj(
      "id" -> 123,
      "name" -> "validSegmentJson",
      "distance" -> 123.4D,
      "average_grade" -> 1.2D,
      "maximum_grade" -> 3.4D,
      "elevation_high" -> 333D,
      "elevation_low" -> 222D,
      "total_elevation_gain" -> 111D,
      "map" -> Json.obj("polyline" -> ""),
      "effort_count" -> 5678,
      "athlete_count" -> 1234,
      "created_at" -> now.toString(DateTimeFormat.fullDateTime),
      "start_latlng" -> Json.arr(-37.1D, 123.4D),
      "end_latlng" -> Json.arr(-37.2D, 123.5D)
    )

    val foundResponse = mock[WSResponse]
    foundResponse.status returns 200
    foundResponse.header(anyString) returns None
    foundResponse.json returns validSegmentJson

    val foundRequest = mock[WSRequest]
    foundRequest.withHeaders(any[(String, String)]) returns foundRequest
    foundRequest.get returns Future.successful(foundResponse)

    mockStravaAPI.segmentUrlFinder(123L) returns foundRequest
    mockStravaAPI.segmentUrlFinder(404L) returns notFoundRequest
    mockStravaAPI.segmentUrlFinder(911L) returns badJsonRequest

    "Return None for non-existent segment" in {
      waitFor(segmentService.getSegment(TOKEN, 404L)) must beNone
    }

    "Return a None if we can't parse a segment" in {
      waitFor(segmentService.getSegment(TOKEN, 911L)) must beNone
    }

    "Return a Some for an existent segment" in {
      waitFor(segmentService.getSegment(TOKEN, 123L)) must beSome
    }
  }

  "StravaSegmentService - getSegmentStream" should {

    val validSegmentStreamJson = Json.arr(
      Json.obj(
        "type" -> "segment",
        "data" -> Json.arr(),
        "series_type" -> "latlng",
        "original_size" -> 999,
        "resolution" -> "HIGH"
      )
    )

    val foundResponse = mock[WSResponse]
    foundResponse.status returns 200
    foundResponse.header(anyString) returns None
    foundResponse.json returns validSegmentStreamJson

    val foundRequest = mock[WSRequest]
    foundRequest.withHeaders(any[(String, String)]) returns foundRequest
    foundRequest.get returns Future.successful(foundResponse)

    mockStravaAPI.segmentStreamUrlFinder(Matchers.eq(123L), anyString, any[Option[String]], any[Option[String]]) returns foundRequest
    mockStravaAPI.segmentStreamUrlFinder(Matchers.eq(404L), anyString, any[Option[String]], any[Option[String]]) returns notFoundRequest
    mockStravaAPI.segmentStreamUrlFinder(Matchers.eq(911L), anyString, any[Option[String]], any[Option[String]]) returns badJsonRequest

    "Return None for non-existent segment stream" in {
      waitFor(segmentService.getSegmentStream(TOKEN, 404L, "streamType", None, None)) must beEmpty
    }

    "Return a Nil if we can't parse a segment stream" in {
      waitFor(segmentService.getSegmentStream(TOKEN, 911L, "streamType", None, None)) must beEmpty
    }

    "Return a Seq for an existent segment stream" in {
      waitFor(segmentService.getSegmentStream(TOKEN, 123L, "streamType", None, None)) must not be empty
    }
  }
}
