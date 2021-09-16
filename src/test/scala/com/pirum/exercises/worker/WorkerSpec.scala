package com.pirum.exercises.worker

import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class WorkerSpec extends ScalaTestWithActorTestKit(ManualTime.config) with AnyWordSpecLike {

  private val manualTime = ManualTime()

  "Worker" should {

    "correctly process successful task" in {
      val worker = testKit.spawn(Worker())
      val probe  = testKit.createTestProbe[Supervisor.Command]

      val task = Task.Completes("success", duration = 5.seconds)
      worker ! Worker.Execute(task, probe.ref)

      manualTime.timePasses(task.duration)
      probe.expectMessage(Supervisor.Successful(task.name, worker))
    }

    "correctly process failed tasks" in {
      val worker = testKit.spawn(Worker())
      val probe  = testKit.createTestProbe[Supervisor.Command]

      val task = Task.Fails("fail", duration = 10.seconds)
      worker ! Worker.Execute(task, probe.ref)

      manualTime.timePasses(task.duration)
      probe.expectMessage(Supervisor.Failed(task.name, worker))
    }

    "not finish processing tasks that hang" in {
      val worker = testKit.spawn(Worker())
      val probe  = testKit.createTestProbe[Supervisor.Command]

      val task = Task.Hangs("hang")
      worker ! Worker.Execute(task, probe.ref)

      manualTime.expectNoMessageFor(1.minute)
    }

  }

}
