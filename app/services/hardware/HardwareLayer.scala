package services.hardware

import akka.stream.scaladsl.{Sink, Source}

trait HardwareLayer {
  def serialSink: Sink[Seq[Byte], _]
  def indicatorSink: Sink[Boolean, _]
  def pressedSource: Source[Boolean, _]
}
