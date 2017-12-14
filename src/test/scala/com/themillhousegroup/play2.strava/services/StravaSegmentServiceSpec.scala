package com.themillhousegroup.play2.strava.services

import com.themillhousegroup.play2.strava.services.helpers.{ AuthBearer, StandardRequestResponseHelper }
import com.themillhousegroup.play2.strava.test.{ StandardMocks, TestFixtures }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsValue, Json }

class StravaSegmentServiceSpec extends Specification with Mockito with TestFixtures with StandardMocks {

  val TOKEN = "Token"

  val now = new DateTime()

  val mockStravaAPI = mock[StravaAPI]

  val requester = new StandardRequestResponseHelper(mockAuthBearer)

  val segmentService = new StravaSegmentService(mockStravaAPI, requester)

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

  "StravaSegmentService - findSegment" should {

    val foundResponse = buildMockResponse(200, Some(validSegmentJson))

    val foundRequest = mockRequestThatReturns(foundResponse)

    mockStravaAPI.segmentUrlFinder(404L) returns notFoundRequest
    mockStravaAPI.segmentUrlFinder(911L) returns badJsonRequest
    mockStravaAPI.segmentUrlFinder(123L) returns foundRequest

    "Fail the future for non-existent segment" in {
      waitFor(segmentService.findSegment(TOKEN, 404L)) must beNone
    }

    "Fail the future if we can't parse a segment" in {
      waitFor(segmentService.findSegment(TOKEN, 911L)) must beNone
    }

    "Return an existent segment" in {
      waitFor(segmentService.findSegment(TOKEN, 123L)) must beSome
    }
  }

  "StravaSegmentService - getSegment" should {

    val foundResponse = buildMockResponse(200, Some(validSegmentJson))

    val foundRequest = mockRequestThatReturns(foundResponse)

    mockStravaAPI.segmentUrlFinder(404L) returns notFoundRequest
    mockStravaAPI.segmentUrlFinder(911L) returns badJsonRequest
    mockStravaAPI.segmentUrlFinder(123L) returns foundRequest

    "Return None for non-existent segment" in {
      waitFor(segmentService.getSegment(TOKEN, 404L)) must throwAn[Exception]
    }

    "Return a None if we can't parse a segment" in {
      waitFor(segmentService.getSegment(TOKEN, 911L)) must throwAn[Exception]
    }

    "Return a Some for an existent segment" in {
      waitFor(segmentService.getSegment(TOKEN, 123L)) must not beNull
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

    val foundResponse = buildMockResponse(200, Some(validSegmentStreamJson))

    val foundRequest = mockRequestThatReturns(foundResponse)

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
