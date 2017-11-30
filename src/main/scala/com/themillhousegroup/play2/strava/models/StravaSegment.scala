package com.themillhousegroup.play2.strava.models

import org.joda.time.DateTime

trait EssentialStravaSegment {
  val id: Long
  val name: String
  val distance: Double
  val average_grade: Double
  val maximum_grade: Double
  val elevation_high: Double
  val elevation_low: Double
  val start_latlng: List[Double]
  val end_latlng: List[Double]

  val startLatitude: Double = start_latlng.headOption.getOrElse(0D)
  val startLongitude: Double = start_latlng.lastOption.getOrElse(0D)
  val endLatitude: Double = end_latlng.headOption.getOrElse(0D)
  val endLongitude: Double = end_latlng.lastOption.getOrElse(0D)
}

case class StravaSegmentSummary(
    id: Long,
    name: String,
    distance: Double,
    average_grade: Double,
    maximum_grade: Double,
    elevation_high: Double,
    elevation_low: Double,
    start_latlng: List[Double],
    end_latlng: List[Double]) extends EssentialStravaSegment {

  lazy val elevationGain: Double = elevation_high - elevation_low

}

case class StravaSegmentMap(polyline: String)

case class StravaSegment(
    id: Long,
    name: String,
    distance: Double,
    average_grade: Double,
    maximum_grade: Double,
    elevation_high: Double,
    elevation_low: Double,
    total_elevation_gain: Double,
    map: StravaSegmentMap,
    effort_count: Int,
    athlete_count: Int,
    created_at: String,
    start_latlng: List[Double],
    end_latlng: List[Double]) extends EssentialStravaSegment {

  lazy val createdAtMillis: Long = DateTime.parse(created_at).getMillis
}

