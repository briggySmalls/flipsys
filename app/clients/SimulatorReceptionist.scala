package clients

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{SinkRef, SourceRef}
import models.simulator.{BytePayload, FlipsysSerializable, StatusPayload}

class SimulatorReceptionist(
    signSource: Source[Seq[Byte], NotUsed],
    indicatorSource: Source[Boolean, NotUsed],
    pressedSink: Sink[Boolean, NotUsed]
) extends Actor
    with ActorLogging {
  import SimulatorReceptionist._

  private implicit val system = context.system
  def receive: Receive = {
    case SimulatorOffer(signsSink, indicatorSink, buttonSource) =>
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
      // Attach the remote source to our pressed events
      buttonSource
        .map(_.status)
        .to(pressedSink)
        .run()
  }
}

object SimulatorReceptionist {
  case class SimulatorOffer(
      signsSink: SinkRef[BytePayload],
      indicatorSink: SinkRef[StatusPayload],
      buttonSource: SourceRef[StatusPayload]
  ) extends FlipsysSerializable
}
