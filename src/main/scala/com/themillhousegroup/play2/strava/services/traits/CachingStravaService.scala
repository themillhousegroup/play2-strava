package com.themillhousegroup.play2.strava.services.traits

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger

trait CachingStravaService[T, ID] {
  val cache: CacheApi
  val logger: Logger

  protected val cacheExpiry = Duration(3, "hours")

  protected def cacheNameFor(entityId: ID): String

  protected def withCacheFor[T: ClassTag](entityId: ID, accessToken: String)(fetcher: (String, ID) => Future[T]): Future[T] = {
    val cacheName = cacheNameFor(entityId)
    val maybeResult = cache.get[T](cacheName)

    maybeResult.fold {
      logger.warn(s"Cache miss for $cacheName")
      fetcher(accessToken, entityId).map { entity =>
        cache.set(cacheName, entity, cacheExpiry)
        entity
      }
    } { hit =>
      logger.debug(s"Cache hit for $cacheName")
      Future.successful(hit)
    }
  }
}
