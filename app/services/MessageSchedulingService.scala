package services

import akka.stream.scaladsl.{Flow, GraphDSL, Keep, MergeHub, Sink, Source}
import akka.stream._
import akka.{Done, NotUsed}
import models.Message
import play.api.Logging

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/** Service to buffer messages awaiting input
  *
  * The MessageSchedulingService emits a stream of messages, latched by an input
  * source. It also emits a stream of indicator signals to show when there are
  * messages waiting to be latched through.
  * @param materializer
  */
class MessageSchedulingService()(implicit materializer: Materializer)
    extends Logging {

  private val (messagesQueue, messagesSource) =
    Source.queue[Message](10).preMaterialize()
  private val gate = new GateFlow[Message](10)
  private val (indicatorSink, indicatorSource) =
    MergeHub.source[Boolean].preMaterialize()

  val graph: Graph[MessageSchedulerShape[Message], NotUsed] =
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val gateGraph = builder.add(gate.graph)
      val latchSink = builder.add(latchOnPress)
      val indicatorCf = builder.add(indicatorControlFlow.to(Sink.ignore))
      val indicatorSrc = builder.add(indicatorSource)

      // Create the main flow: messages that are gated
      messagesSource ~> gateGraph.in

      // Add logic for controlling the indicator
      gateGraph.status ~> indicatorCf

      MessageSchedulerShape(
        latchSink.in,
        gateGraph.out,
        indicatorSrc.out
      )
    }

  def message(message: Message): Unit = {
    messagesQueue.offer(message)
  }

  private def latchOnPress: Sink[Boolean, Future[Done]] =
    Sink.foreach((b: Boolean) =>
      if (b) {
        val isLatched = gate.latch()
        logger.info(s"latch() = ${isLatched}")
      }
    )

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
      .to(indicatorSink)
      .run()
  }
}

object MessageSchedulingService {
  trait ButtonEvent
  object ActivateEvent extends ButtonEvent
  object PressedEvent extends ButtonEvent
}
