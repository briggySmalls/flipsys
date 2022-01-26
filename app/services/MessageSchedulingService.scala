package services

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import models.Message
import play.api.Logging

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class MessageSchedulingService(
    private val pressed: Source[Boolean, _],
    private val indicator: Sink[Boolean, _]
)(implicit materializer: Materializer)
    extends Logging {
  import MessageSchedulingService._

  private var messages = Queue[Message]()

  private val activateSource = Source.queue[Unit](16)
  private val (activateSourceQueue, materializedActivateSource) =
    activateSource.preMaterialize()
  private val eventSource = materializedActivateSource
    .map(_ => ActivateEvent)
    .merge(
      pressed.map(_ => PressedEvent)
    )

  val requestSource: Source[Message, _] = eventSource
    .via(onOffLatchFlow)
    .via(indicatorControlFlow)
    .filter(_ == PressedEvent)
    .map(e => (dequeue().toOption))
    .collect { case Some(msg) => msg }

  def message(message: Message): Unit = {
    messages = messages.enqueue(message)
    logger.info("queuing activation")
    activateSourceQueue.offer()
  }

  private def dequeue(): Try[Message] = {
    for {
      (msg, tail) <- Try(messages.dequeue)
    } yield {
      messages = tail
      msg
    }
  }

  private def onOffLatchFlow =
    Flow[ButtonEvent]
      .statefulMapConcat { () =>
        var state: ButtonEvent = PressedEvent

        {
          case e if (e == state) =>
            Nil // Filter out duplicate events
          case e => {
            state = e
            e :: Nil
          }
        }
      }

  private def indicatorControlFlow =
    Flow[ButtonEvent]
      .statefulMapConcat { () =>
        var maybeKs: Option[KillSwitch] = None

        {
          case e @ ActivateEvent =>
            logger.info("Starting indicator stream")
            maybeKs = Some(runIndicatorStream())
            e :: Nil
          case e @ PressedEvent =>
            logger.info("Stopping indicator stream")
            maybeKs.foreach(_.shutdown())
            e :: Nil
        }
      }

  private def runIndicatorStream() = {
    Source
      .tick(0 second, 1 second, None)
      .viaMat(KillSwitches.single)(Keep.right)
      .statefulMapConcat { () =>
        var status = false

        _ => {
          status = !status
          status :: Nil
        }
      }
      .to(indicator)
      .run()
  }
}

object MessageSchedulingService {
  trait ButtonEvent
  object ActivateEvent extends ButtonEvent
  object PressedEvent extends ButtonEvent
}
