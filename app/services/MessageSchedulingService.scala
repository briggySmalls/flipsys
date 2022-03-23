package services

import akka.NotUsed
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{
  KillSwitch,
  KillSwitches,
  Materializer,
  SourceShape,
  UniqueKillSwitch
}
import models.Message
import play.api.Logging

import scala.concurrent.duration._
import scala.language.postfixOps

class MessageSchedulingService(
    private val pressed: Source[Boolean, _],
    private val indicator: Sink[Boolean, _]
)(implicit materializer: Materializer)
    extends Logging {

  private val (messagesQueue, messagesSource) =
    Source.queue[Message](10).preMaterialize()
  private val gate = new GateFlow[Message](10)

  val requestSource: Source[Message, NotUsed] = Source.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val gateGraph = builder.add(gate.graph)
      val latchSink =
        builder.add(Sink.foreach((b: Boolean) => if (b) gate.latch()))
      val indicatorCf = builder.add(indicatorControlFlow.to(Sink.ignore))

      // Create the main flow: messages that are gated
      messagesSource ~> gateGraph.in

      // Control the gate
      pressed ~> latchSink

      // Add logic for controlling the indicator
      gateGraph.status ~> indicatorCf

      SourceShape.of(gateGraph.out)
    }
  )

  def message(message: Message): Unit = {
    messagesQueue.offer(message)
  }

  private def indicatorControlFlow =
    Flow[GateFlow.Status]
      .scan[Option[KillSwitch]](None) { (maybeKs, status) =>
        status match {
          case GateFlow.Empty =>
            logger.info(s"Stopping indicator stream: ${maybeKs}")
            maybeKs.foreach(_.shutdown())
            None
          case GateFlow.NonEmpty =>
            logger.info("Starting indicator stream")
            Some(runIndicatorStream())
        }
      }

  private def runIndicatorStream(): UniqueKillSwitch = {
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
