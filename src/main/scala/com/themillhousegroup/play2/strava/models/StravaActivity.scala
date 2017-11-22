package com.themillhousegroup.play2.strava.models

import play.api.libs.json.{ JsValue, Json }
import org.joda.time.{ DateTimeZone, DateTime, Days }
import org.apache.commons.lang3.StringUtils
import org.joda.time

object StravaActivity {
  val tzRegex = """^\(.*\)[ ](.*)$""".r

  def parseToJavaTimeZone(stravaTimezone: String): String =
    tzRegex.findFirstMatchIn(stravaTimezone).fold("UTC") { m =>
      m.group(1)
    }
}

trait EssentialStravaActivity extends EssentialStravaEntity {
  val name: String
  val distance: Double
  val manual: Boolean
  val commute: Boolean
  val `private`: Boolean
  val start_date: String
  val timezone: String
  val `type`: String
  val total_elevation_gain: Double
  val moving_time: Int
  val elapsed_time: Int

  private val millis: Long = DateTime.parse(start_date).getMillis
  private val javaTZ: String = StravaActivity.tzRegex.findFirstMatchIn(timezone).fold("UTC") { m =>
    m.group(1)
  }

  lazy val at = new time.DateTime(millis, DateTimeZone.forID(javaTZ))

  val movingTimeSeconds = moving_time
  val totalTimeSeconds = elapsed_time

  val verticalMetres = Option(total_elevation_gain)

  lazy val distanceKm = distance / 1000
}

case class StravaActivitySummary(
  id: Long,
  name: String,
  distance: Double,
  `type`: String,
  start_date: String,
  timezone: String,
  moving_time: Int,
  elapsed_time: Int,
  total_elevation_gain: Double,
  manual: Boolean,
  commute: Boolean,
  `private`: Boolean) extends EssentialStravaActivity

case class StravaActivity(
    id: Long,
    athlete: MinimalStravaEntity,
    name: String,
    distance: Double,
    moving_time: Int,
    elapsed_time: Int,
    total_elevation_gain: Double,
    `type`: String,
    start_date: String,
    timezone: String,
    location_city: Option[String],
    location_state: Option[String],
    location_country: Option[String],
    manual: Boolean,
    commute: Boolean,
    `private`: Boolean,
    description: Option[String] = None,
    segment_efforts: Option[List[StravaSegmentEffort]] = None) extends EssentialStravaActivity {

}

/** This version sacrifices location fields for performance aspects */
case class StravaActivityPerformanceAspects(
    id: Long,
    athlete: MinimalStravaEntity,
    name: String,
    distance: Double,
    moving_time: Int,
    elapsed_time: Int,
    total_elevation_gain: Double,
    `type`: String,
    start_date: String,
    timezone: String,
    average_speed: Double,
    max_speed: Double,
    average_watts: Double,
    max_watts: Option[Double],
    suffer_score: Option[Int],
    kudos_count: Int,
    manual: Boolean,
    commute: Boolean,
    `private`: Boolean,
    segment_efforts: Option[List[StravaSegmentEffort]] = None) extends EssentialStravaActivity {
}

