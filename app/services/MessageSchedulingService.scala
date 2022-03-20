package services

import akka.NotUsed
import akka.stream.scaladsl.{
  Broadcast,
  Flow,
  GraphDSL,
  Keep,
  Merge,
  Sink,
  Source
}
import akka.stream.{KillSwitch, KillSwitches, Materializer, SourceShape}
import models.Message
import play.api.Logging

import scala.concurrent.duration._
import scala.language.postfixOps

class MessageSchedulingService(
    private val pressed: Source[Boolean, _],
    private val indicator: Sink[Boolean, _]
)(implicit materializer: Materializer)
    extends Logging {
  import MessageSchedulingService._

  private val (messagesQueue, messagesSource) =
    Source.queue[Message](10).preMaterialize()
  private val gate = new GateFlow[Message](10)

  val requestSource: Source[Message, NotUsed] = Source.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val gatedFlow = builder.add(gate.flow)
      val bCastMessages = builder.add(Broadcast[Message](2))
      val bCastPressed = builder.add(Broadcast[Boolean](2))
      val latchSink =
        builder.add(Sink.foreach((b: Boolean) => if (b) gate.latch()))
      val msg2Event = builder.add(Flow[Message].map(_ => ActivateEvent))
      val press2Event = builder.add(Flow[Boolean].map(_ => PressedEvent))
      val merge = builder.add(Merge[ButtonEvent](2))
      val onOff = builder.add(onOffLatchFlow)
      val indicatorCf = builder.add(indicatorControlFlow.to(Sink.ignore))

      // Create the main flow: messages that are gated
      messagesSource ~> bCastMessages.in
      bCastMessages.out(0) ~> gatedFlow

      // Control the gate
      pressed ~> bCastPressed.in
      bCastPressed.out(0) ~> latchSink

      // Add logic for controlling the indicator
      bCastMessages.out(1) ~> msg2Event ~> merge.in(0)
      bCastPressed.out(1) ~> press2Event ~> merge.in(1)
      merge.out ~> onOff ~> indicatorCf

      SourceShape.of(gatedFlow.out)
    }
  )

  def message(message: Message): Unit = {
    messagesQueue.offer(message)
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
      .scan[Option[KillSwitch]](None) { (maybeKs, e) =>
        e match {
          case ActivateEvent =>
            logger.info("Starting indicator stream")
            Some(runIndicatorStream())
          case PressedEvent =>
            logger.info("Stopping indicator stream")
            maybeKs.foreach(_.shutdown())
            None
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
