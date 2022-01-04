package services.hardware

import akka.NotUsed
import akka.stream.scaladsl.Sink
import config.hardware.SimulatedHardwareSettings
import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class SimulatedHardwareImpl @Inject() (settings: SimulatedHardwareSettings)
    extends HardwareLayer {
  def serialSink = Sink.ignore
}
