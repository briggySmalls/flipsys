package services

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import play.api.Logging

import scala.concurrent.duration._
import scala.language.postfixOps

class ButtonService(
    private val activate: Source[Unit, _],
    private val pressed: Source[Boolean, _],
    private val indicator: Sink[Boolean, _]
)(implicit materializer: Materializer)
    extends Logging {
  import ButtonService._

  private val eventSource = activate
    .map(_ => ActivateEvent)
    .merge(
      pressed.map(_ => PressedEvent)
    )

  val requestSource: Source[Unit, _] = eventSource
    .via(onOffLatchFlow)
    .via(indicatorControlFlow)
    .filter(_ == PressedEvent)
    .map(e => ())

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

object ButtonService {
  trait ButtonEvent
  object ActivateEvent extends ButtonEvent
  object PressedEvent extends ButtonEvent
}
