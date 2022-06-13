package services.hardware

import akka.stream.scaladsl.{Sink, Source}
import clients.SerializerSink
import com.pi4j.Pi4J
import config.hardware.RealHardwareSettings

import javax.inject.{Inject, Singleton}

@Singleton
class RealHardwareImpl @Inject() (settings: RealHardwareSettings)
    extends HardwareLayer {
  private val pi4j = Pi4J.newAutoContext
  private val INDICATOR_PIN = 24 // PIN 18 = BCM 24

  import com.pi4j.io.gpio.digital.DigitalOutput
  import com.pi4j.io.gpio.digital.DigitalState

  val ledConfig = DigitalOutput
    .newConfigBuilder(pi4j)
    .id("led")
    .name("Button Led")
    .address(INDICATOR_PIN)
    .shutdown(DigitalState.LOW)
    .initial(DigitalState.LOW)
    .provider("pigpio-digital-output")

  val led = pi4j.create(ledConfig)

  override def serialSink: Sink[Seq[Byte], _] = SerializerSink(settings.port)

  override def indicatorSink: Sink[Boolean, _] =
    Sink.foreach(b => led.setState(b))

  override def pressedSource: Source[Boolean, _] = ???
}
