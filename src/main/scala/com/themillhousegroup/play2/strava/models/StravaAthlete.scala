package com.themillhousegroup.play2.strava.models

trait EssentialStravaAthlete {
  val id: Long
  val firstname: String
  val lastname: String
  val sex: Option[String]
  val profile_medium: String
  val profile: String

  lazy val isMale: Option[Boolean] = sex.map("M" == _)

  lazy val name = s"$firstname $lastname"
}

case class StravaAthleteSummary(
  id: Long,
  resource_state: Int,
  firstname: String,
  lastname: String,
  sex: Option[String],
  profile_medium: String,
  profile: String) extends EssentialStravaAthlete

case class StravaAthlete(
  id: Long,
  resource_state: Int,
  firstname: String,
  lastname: String,
  sex: Option[String],
  profile_medium: String,
  profile: String) extends EssentialStravaAthlete
