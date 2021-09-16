package com.pirum.exercises.worker

import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class SupervisorSpec extends ScalaTestWithActorTestKit(ManualTime.config) with AnyWordSpecLike {

  val tasks = List(
    Task.Fails(name = "Task3", duration = 3.seconds),
    Task.Hangs(name = "Task5"),
    Task.Fails(name = "Task6", duration = 4.seconds),
    Task.Completes(name = "Task4", duration = 4.seconds),
    Task.Completes(name = "Task2", duration = 2.seconds),
    Task.Completes(name = "Task1", duration = 1.second)
  )

  private val manualTime = ManualTime()

  "Supervisor" should {

    "send response back after timeout" in {
      val supervisor = testKit.spawn(Supervisor())
      val client     = testKit.createTestProbe[Supervisor.Response]

      supervisor ! Supervisor.Request(tasks = Nil, numberOfWorkers = 0, replyTo = client.ref, timeout = 3.seconds)

      manualTime.expectNoMessageFor(2.seconds)

      manualTime.timePasses(1.second)
      client.expectMessage(Supervisor.Response())
    }

    "receive tasks back in order of their duration" in {
      val supervisor = testKit.spawn(Supervisor())
      val client     = testKit.createTestProbe[Supervisor.Response]

      supervisor ! Supervisor.Request(tasks, numberOfWorkers = 4, replyTo = client.ref, timeout = 8.seconds)

      (1 to 5).foreach { _ =>
        manualTime.timePasses(1.second)
      }

      manualTime.timePasses(3.seconds)

      client.expectMessage(
        Supervisor.Response(
          successful = List("Task1", "Task2", "Task4"),
          failed = List("Task3", "Task6"),
          timedOut = List("Task5")
        )
      )
    }

  }

}
