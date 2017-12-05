package com.themillhousegroup.play2.strava.test

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration.Duration

trait TestFixtures {

  val timeout = Duration(20, "seconds")

  def waitFor[T](awaitable: Awaitable[T]) = Await.result(awaitable, timeout)
}
