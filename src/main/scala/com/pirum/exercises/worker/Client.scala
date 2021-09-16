package com.pirum.exercises.worker

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Success}

object Client {

  sealed trait Command

  final case class Response(response: Supervisor.Response) extends Command

  def apply(tasks: List[Task], numberOfWorkers: Int, timeout: FiniteDuration): Behavior[Response] =
    Behaviors.setup { context =>
      implicit val askTimeout: Timeout = timeout + 500.millis

      val supervisor = context.spawn(Supervisor(), "supervisor")

      context.ask(supervisor, Supervisor.Request(tasks, numberOfWorkers, _, timeout)) {
        case Success(response) => Response(response)
        case Failure(exception) =>
          context.log.error(exception.getMessage, exception)
          Response(Supervisor.Response())
      }

      handleResponse(context)
    }

  private def handleResponse(context: ActorContext[Response]) =
    Behaviors.receiveMessage[Response] {
      case Response(Supervisor.Response(successful, failed, timedOut)) =>
        context.log.info(s"Successful: ${successful.mkString("[", ", ", "]")}")
        context.log.info(s"Failed: ${failed.mkString("[", ", ", "]")}")
        context.log.info(s"Timed Out: ${timedOut.mkString("[", ", ", "]")}")

        Behaviors.stopped
    }

}
