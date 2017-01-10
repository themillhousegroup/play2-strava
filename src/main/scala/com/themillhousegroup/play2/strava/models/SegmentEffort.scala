package com.themillhousegroup.play2.strava.models

import org.joda.time.DateTime

trait EssentialStravaSegmentEffort {
  val id: Long
  val name: String
  val activity: MinimalStravaEntity
  val start_date: String
  val segment: StravaSegmentSummary

  lazy val at = DateTime.parse(start_date)

  lazy val millis: Long = at.getMillis
}

case class StravaSegmentEffort(id: Long,
  name: String,
  activity: MinimalStravaEntity,
  start_date: String,
  segment: StravaSegmentSummary,
  moving_time: Int,
  average_watts: Double) extends EssentialStravaSegmentEffort
