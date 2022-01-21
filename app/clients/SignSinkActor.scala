package clients

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.stream.scaladsl.Source
import models.simulator.{BytePayload, SignSinkOffer}

class SignSinkActor(source: Source[Seq[Byte], NotUsed])
    extends Actor
    with ActorLogging {
  private implicit val system = context.system
  def receive: Receive = { case SignSinkOffer(sinkRef) =>
    log.info(s"Received remote sink: $sinkRef, connecting to $source")
    // Attach the remote sink to our signs
    source
      .map(b => BytePayload(b.toArray))
      .to(sinkRef)
      .run()
  }
}
