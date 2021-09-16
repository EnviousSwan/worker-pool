package com.pirum.exercises.worker

import akka.actor.ActorSystem
import akka.pattern.after

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

// A task that either succeeds after n seconds, fails after n seconds, or never terminates
sealed trait Task {
  def name: String
  def execute(implicit system: ActorSystem): Future[Unit]
}

object Task {

  final case class Completes(name: String, duration: FiniteDuration) extends Task {
    def execute(implicit system: ActorSystem): Future[Unit] = after(duration)(Future.unit)
  }

  final case class Fails(name: String, duration: FiniteDuration) extends Task {
    def execute(implicit system: ActorSystem): Future[Unit] = after(duration)(Future.failed(new RuntimeException))
  }

  final case class Hangs(name: String) extends Task {
    def execute(implicit system: ActorSystem): Future[Unit] = Future.never
  }

}
