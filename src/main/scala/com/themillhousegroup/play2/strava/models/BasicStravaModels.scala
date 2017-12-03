package com.themillhousegroup.play2.strava.models

import play.api.libs.json.{ JsValue, Json }

case class MinimalStravaEntity(
  id: Long,
  resource_state: Int)

case class StravaStreamObject(`type`: String,
  data: List[JsValue],
  series_type: String,
  original_size: Int,
  resolution: String)

trait StravaJson {
  implicit val minimalStravaEntityFormat = Json.format[MinimalStravaEntity]
}

object StravaAthleteJson {
  implicit val stravaAthleteFormat = Json.format[StravaAthlete]
}

object StravaPhotoJson {
  implicit val stravaPhotoReads = Json.reads[StravaActivityPhoto]
  implicit val instaPhotoReads = Json.reads[InstagramActivityPhoto]
}

object StravaStreamJson {
  implicit val stravaStreamObjectFormat = Json.format[StravaStreamObject]
}

object StravaSegmentJson extends StravaJson {
  implicit val stravaSegmentMapFormat = Json.format[StravaSegmentMap]
  implicit val stravaSegmentFormat = Json.format[StravaSegment]
  implicit val stravaSegmentSummaryFormat = Json.format[StravaSegmentSummary]
  implicit val stravaSegmentEffortFormat = Json.format[StravaSegmentEffort]
}

object StravaActivityJson extends StravaJson {
  import StravaSegmentJson.stravaSegmentEffortFormat

  implicit val stravaActivityFormat = Json.format[StravaActivity]
}

object StravaActivityPerformanceAspectsJson extends StravaJson {
  import StravaSegmentJson.stravaSegmentEffortFormat

  implicit val stravaActivityPerformanceFormat = Json.format[StravaActivityPerformanceAspects]
}

object StravaActivitySummaryJson extends StravaJson {
  implicit val stravaActivitySummaryFormat = Json.format[StravaActivitySummary]
}
