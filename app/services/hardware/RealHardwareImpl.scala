package services.hardware

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import clients.SerializerSink
import com.pi4j.Pi4J
import com.pi4j.io.gpio.digital.{DigitalInput, DigitalOutput, DigitalState, PullResistance}
import config.hardware.RealHardwareSettings

import javax.inject.{Inject, Singleton}

@Singleton
class RealHardwareImpl @Inject() (settings: RealHardwareSettings)(implicit materializer: Materializer)
    extends HardwareLayer {
  private val pi4j = Pi4J.newAutoContext
  private val INDICATOR_PIN = 24 // PIN 18 = BCM 24
  private val BUTTON_PIN = 6 //

  private val led = pi4j.create(DigitalOutput
    .newConfigBuilder(pi4j)
    .id("led")
    .name("Button Led")
    .address(INDICATOR_PIN)
    .shutdown(DigitalState.LOW)
    .initial(DigitalState.LOW)
    .provider("pigpio-digital-output"))

  private val button = pi4j.create(DigitalInput
    .newConfigBuilder(pi4j)
    .id("led")
    .name("Button")
    .pull(PullResistance.PULL_DOWN)
    .address(BUTTON_PIN)
    .provider("pigpio-digital-output"))

  button.addListener(e =>
    e.state() match {
      case DigitalState.LOW => queue.offer(false)
      case DigitalState.HIGH => queue.offer(true)
    }
  )

  private val (queue, source) = Source.queue[Boolean](10).preMaterialize()

  override def serialSink: Sink[Seq[Byte], _] = SerializerSink(settings.port)

  override def indicatorSink: Sink[Boolean, _] =
    Sink.foreach(b => led.setState(b))

  override def pressedSource: Source[Boolean, _] = source
}
