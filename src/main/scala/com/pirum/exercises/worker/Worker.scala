package com.pirum.exercises.worker

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.util.{Failure, Success}

object Worker {

  sealed trait Command

  case object Stop extends Command

  final case class Execute(task: Task, supervisor: ActorRef[Supervisor.Command]) extends Command

  private final case class Successful(name: String, supervisor: ActorRef[Supervisor.Command]) extends Command
  private final case class Failed(name: String, supervisor: ActorRef[Supervisor.Command])     extends Command

  def apply(): Behavior[Command] =
    Behaviors.receive {
      case (_, Stop) => Behaviors.stopped

      case (context, Execute(task, supervisor)) =>
        context.log.info(s"Got new task ${task.name}, executing")

        context.pipeToSelf(task.execute(context.system.classicSystem)) {
          case Failure(_) => Failed(task.name, supervisor)
          case Success(_) => Successful(task.name, supervisor)
        }

        Behaviors.same

      case (context, Successful(taskName, supervisor)) =>
        context.log.info(s"Finished processing task $taskName successfully")
        supervisor ! Supervisor.Successful(taskName, context.self)

        Behaviors.same

      case (context, Failed(taskName, supervisor)) =>
        context.log.info(s"Finished processing task $taskName with error")
        supervisor ! Supervisor.Failed(taskName, context.self)

        Behaviors.same
    }

}
