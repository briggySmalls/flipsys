package models.simulator

import akka.stream.SinkRef

/** Message for offering remote stream details from a simulator instance
  * @param sink
  *   The remote sink
  */
case class SignSinkOffer(sink: SinkRef[BytePayload]) extends FlipsysSerializable
