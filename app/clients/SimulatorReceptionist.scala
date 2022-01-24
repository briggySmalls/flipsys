package clients

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.stream.SinkRef
import akka.stream.scaladsl.{Sink, Source, StreamRefs}
import models.simulator.{BytePayload, FlipsysSerializable, StatusPayload}

class SimulatorReceptionist(
    signSource: Source[Seq[Byte], NotUsed],
    indicatorSource: Source[Boolean, NotUsed],
    pressedSink: Sink[Boolean, NotUsed]
) extends Actor
    with ActorLogging {
  import SimulatorReceptionist._

  private implicit val system = context.system
  def receive: Receive = { case SimulatorOffer(signsSink, indicatorSink) =>
    log.info(
      s"Received signSink: $signsSink, indicatorSink: $indicatorSink" +
        s"connecting to $signSource, $indicatorSource"
    )
    // Attach the remote sink to our signs
    signSource
      .map(b => BytePayload(b.toArray))
      .to(signsSink)
      .run()
    // Attach the remote sink to our indicator
    indicatorSource
      .map(b => StatusPayload(b))
      .to(indicatorSink)
      .run()
    // Provide our sink for pressed events
    val pressedSinkRef =
      StreamRefs
        .sinkRef[StatusPayload]()
        .map(_.status)
        .to(pressedSink)
        .run()
    sender() ! SimulatorOfferAck(pressedSinkRef)
  }
}

object SimulatorReceptionist {
  case class SimulatorOffer(
      signsSink: SinkRef[BytePayload],
      indicatorSink: SinkRef[StatusPayload]
  ) extends FlipsysSerializable

  case class SimulatorOfferAck(pressedSink: SinkRef[StatusPayload])
      extends FlipsysSerializable
}
