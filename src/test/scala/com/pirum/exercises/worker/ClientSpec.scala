package com.pirum.exercises.worker

import akka.actor.testkit.typed.scaladsl.{LoggingTestKit, ScalaTestWithActorTestKit}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class ClientSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {

  "Client" should {

    "correctly log the supervisor response" in {
      val client = testKit.spawn(Client(tasks = List.empty, numberOfWorkers = 1, timeout = 1.second))

      LoggingTestKit.info("Successful: [Task1, Task3]").expect {
        client ! Client.Response(Supervisor.Response(successful = List("Task1", "Task3")))
      }

      LoggingTestKit.info("Failed: [Task2, Task4]").expect {
        client ! Client.Response(Supervisor.Response(failed = List("Task2", "Task4")))
      }

      LoggingTestKit.info("Timed Out: []").expect {
        client ! Client.Response(Supervisor.Response())
      }
    }

  }

}
