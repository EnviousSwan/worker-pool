package com.pirum.exercises.worker

import akka.actor.typed

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Main {

  def main(args: Array[String]): Unit = {
    val workers                 = 4
    val timeout: FiniteDuration = 1.seconds
    
    val tasks = List(
      Task.Fails(name = "Task3", duration = 3.seconds),
      Task.Hangs(name = "Task5"),
      Task.Completes(name = "Task4", duration = 4.seconds),
      Task.Completes(name = "Task2", duration = 2.seconds),
      Task.Completes(name = "Task1", duration = 1.second)
    )

    typed.ActorSystem(Client(tasks, workers, timeout), "client")
  }

}
