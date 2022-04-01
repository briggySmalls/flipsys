package services.hardware

import akka.stream.scaladsl.{Sink, Source}
import clients.SerializerSink
import config.hardware.RealHardwareSettings

import javax.inject.{Inject, Singleton}

@Singleton
class RealHardwareImpl @Inject() (settings: RealHardwareSettings)
    extends HardwareLayer {
  override def serialSink: Sink[Seq[Byte], _] = SerializerSink(settings.port)

  override def indicatorSink: Sink[Boolean, _] = ???

  override def pressedSource: Source[Boolean, _] = ???
}
