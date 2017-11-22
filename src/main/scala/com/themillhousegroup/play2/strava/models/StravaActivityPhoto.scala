package com.themillhousegroup.play2.strava.models

import org.joda.time.DateTime
import play.api.libs.json.{ JsResult, Reads, _ }

import scala.util.matching.Regex.Match

trait EssentialStravaActivityPhoto extends Comparable[EssentialStravaActivityPhoto] {

  val source: Int // (1 for Strava, 2 for Instagram)

  val urls: Map[String, String]

  val caption: String
  val uploaded_at: String
  val created_at: String

  lazy val uploadedAt = DateTime.parse(uploaded_at)
  lazy val createdAt = DateTime.parse(created_at)

  def compareTo(otherESAP: EssentialStravaActivityPhoto): Int = {
    val u = this.uploadedAt.compareTo(otherESAP.uploadedAt)
    if (u == 0) {
      this.createdAt.compareTo(otherESAP.createdAt)
    } else {
      u
    }
  }

  lazy val hashtags: Set[String] = EssentialStravaActivityPhoto.hashtagPattern.findAllMatchIn(caption).toSet[Match].map(_.matched)
}

object EssentialStravaActivityPhoto {

  val hashtagPattern = """([#][\S]+)""".r

  implicit val esapWrites = new Writes[EssentialStravaActivityPhoto] {
    override def writes(esap: EssentialStravaActivityPhoto): JsValue = {

      val sourceName = esap.source match {
        case 2 => "INSTA"
        case _ => "STRAVA"
      }

      val urlMap = esap.urls.map {
        case (k, v) =>
          k -> JsString(v)
      }

      Json.obj(
        "source" -> esap.source,
        "sourceName" -> sourceName,
        "caption" -> esap.caption,
        "createdAt" -> esap.createdAt,
        "uploadedAt" -> esap.uploadedAt,
        "urls" -> JsObject(urlMap)
      )
    }
  }

  implicit val esapReads = new Reads[EssentialStravaActivityPhoto] {
    override def reads(json: JsValue): JsResult[EssentialStravaActivityPhoto] = {
      (json \ "source").validate[Int].flatMap { src =>
        src match {
          case 2 => StravaJson.instaPhotoReads.reads(json)
          case _ => StravaJson.stravaPhotoReads.reads(json)
        }
      }
    }
  }
}

case class StravaActivityPhoto(
  source: Int,
  unique_id: Option[String],
  urls: Map[String, String],
  caption: String,
  uploaded_at: String,
  created_at: String,
  location: Option[Seq[String]]) extends EssentialStravaActivityPhoto

case class InstagramActivityPhoto(
  id: Long,
  source: Int,
  ref: String,
  uid: String,
  urls: Map[String, String],
  `type`: String,
  caption: String,
  uploaded_at: String,
  created_at: String,
  location: Option[Seq[String]]) extends EssentialStravaActivityPhoto