package com.themillhousegroup.play2.strava.models

import org.joda.time.{ DateTime, LocalDateTime }

trait EssentialStravaSegmentEffort {
  val id: Long
  val name: String
  val activity: MinimalStravaEntity
  val start_date: String
  val start_date_local: String
  val segment: StravaSegmentSummary

  lazy val at = DateTime.parse(start_date)

  lazy val millis: Long = at.getMillis

  lazy val atLocal = LocalDateTime.parse(start_date_local)
}

case class StravaSegmentEffort(id: Long,
  name: String,
  activity: MinimalStravaEntity,
  start_date: String,
  start_date_local: String,
  segment: StravaSegmentSummary,
  moving_time: Int,
  average_watts: Double) extends EssentialStravaSegmentEffort
