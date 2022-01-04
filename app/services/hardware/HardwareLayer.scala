package services.hardware

import akka.NotUsed
import akka.stream.scaladsl.Sink

trait HardwareLayer {
  def serialSink: Sink[Seq[Byte], _]
}
