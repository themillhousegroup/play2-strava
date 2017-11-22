package com.themillhousegroup.play2.strava.models

import play.api.libs.json.{ JsValue, Json }

trait EssentialStravaEntity {
  val id: Long
}

case class MinimalStravaEntity(
  id: Long,
  resource_state: Int) extends EssentialStravaEntity

case class StravaStreamObject(`type`: String,
  data: List[JsValue],
  series_type: String,
  original_size: Int,
  resolution: String)

object StravaJson {
  implicit val minimalStravaEntityFormat = Json.format[MinimalStravaEntity]
  implicit val stravaAthleteFormat = Json.format[StravaAthlete]
  implicit val stravaAthleteSummaryFormat = Json.format[StravaAthleteSummary]
  implicit val stravaActivitySummaryFormat = Json.format[StravaActivitySummary]
  implicit val stravaSegmentMapFormat = Json.format[StravaSegmentMap]
  implicit val stravaSegmentFormat = Json.format[StravaSegment]
  implicit val stravaSegmentSummaryFormat = Json.format[StravaSegmentSummary]
  implicit val stravaSegmentEffortFormat = Json.format[StravaSegmentEffort]
  implicit val stravaActivityFormat = Json.format[StravaActivity]
  implicit val stravaActivityPerformanceFormat = Json.format[StravaActivityPerformanceAspects]

  implicit val stravaStreamObjectFormat = Json.format[StravaStreamObject]

  val stravaPhotoReads = Json.reads[StravaActivityPhoto]
  val instaPhotoReads = Json.reads[InstagramActivityPhoto]
}
