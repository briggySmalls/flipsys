package services.hardware

import akka.stream.scaladsl.Sink

trait HardwareLayer {
  def serialSink: Sink[Seq[Byte], _]
}
