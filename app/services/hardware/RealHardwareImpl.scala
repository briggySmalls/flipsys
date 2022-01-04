package services.hardware

import akka.NotUsed
import akka.stream.scaladsl.Sink
import clients.SerializerSink
import config.hardware.RealHardwareSettings

import javax.inject.{Inject, Singleton}

@Singleton
class RealHardwareImpl @Inject() (settings: RealHardwareSettings)
    extends HardwareLayer {
  def serialSink: Sink[Seq[Byte], _] = SerializerSink(settings.port)
}
