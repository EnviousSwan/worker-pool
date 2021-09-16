package com.pirum.exercises.worker

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration
import monocle.syntax.all.toAppliedFocusOps

object Supervisor {

  sealed trait Command

  final case class Request(
      tasks: List[Task],
      numberOfWorkers: Int,
      replyTo: ActorRef[Response],
      timeout: FiniteDuration
  ) extends Command

  final case class Response(
      successful: List[String] = Nil,
      failed: List[String] = Nil,
      timedOut: List[String] = Nil
  )

  sealed trait TaskResult extends Command {
    def name: String
    def worker: ActorRef[Worker.Command]
  }

  final case class Successful(name: String, worker: ActorRef[Worker.Command]) extends TaskResult
  final case class Failed(name: String, worker: ActorRef[Worker.Command])     extends TaskResult

  private case object Timeout extends Command

  final case class State(
      remainingTasks: Queue[Task] = Queue.empty,
      replyTo: Option[ActorRef[Response]] = None,
      inFlightTasks: Map[String, Long] = Map.empty,
      successful: Map[Successful, Long] = Map.empty,
      failed: Map[Failed, Long] = Map.empty
  )

  def apply(): Behavior[Command] = processing(State())

  def processing(state: State): Behavior[Command] =
    Behaviors.receive[Command] {

      case (context, Request(tasks, numberOfWorkers, replyTo, timeout)) =>
        Behaviors.withTimers { timer =>
          val startTime = System.currentTimeMillis()

          timer.startSingleTimer(Timeout, timeout)

          val (initialTasks, remainingTasks) = tasks.splitAt(numberOfWorkers)
          initialTasks.zipWithIndex.foreach { case (task, i) =>
            val worker = context.spawn(Worker(), s"worker-$i")
            worker ! Worker.Execute(task, context.self)
          }

          val inFlightTasks = initialTasks.map(_.name -> startTime).toMap

          timer.startSingleTimer(Timeout, timeout)

          processing(State(Queue.from(remainingTasks), replyTo = Some(replyTo), inFlightTasks))
        }

      case (context, result: TaskResult) =>
        val (_, freeWorker) = (result.name, result.worker)

        val completedTaskState = handleCompletedTask(state, result)

        if (completedTaskState.remainingTasks.nonEmpty) {
          val (newTask, remainingTasks) = completedTaskState.remainingTasks.dequeue
          freeWorker ! Worker.Execute(newTask, context.self)

          val newRunningTaskState = completedTaskState
            .copy(remainingTasks = remainingTasks)
            .focus(_.inFlightTasks)
            .modify(_ + (newTask.name -> System.currentTimeMillis()))

          processing(newRunningTaskState)
        } else {
          if (completedTaskState.inFlightTasks.isEmpty)
            respond(completedTaskState)
          else {
            freeWorker ! Worker.Stop
            processing(completedTaskState)
          }
        }

      case (context, Timeout) =>
        context.log.info("Task processing timed out")
        respond(state)
    }

  private def handleCompletedTask(state: State, result: TaskResult): State = {
    val taskDuration = System.currentTimeMillis() - state.inFlightTasks(result.name)

    val updated = result match {
      case successful: Successful => state.focus(_.successful).modify(_ + (successful -> taskDuration))
      case failed: Failed         => state.focus(_.failed).modify(_ + (failed -> taskDuration))
    }

    updated.focus(_.inFlightTasks).modify(_ - result.name)
  }

  private def respond(state: State): Behavior[Command] = {
    state.replyTo.foreach {
      _ ! Response(
        successful = state.successful.toList.sortBy(_._2).map(_._1.name),
        failed = state.failed.toList.sortBy(_._2).map(_._1.name),
        timedOut = state.remainingTasks.map(_.name).toList ::: state.inFlightTasks.keys.toList
      )
    }

    Behaviors.stopped
  }

}
