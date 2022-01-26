package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import config.ApplicationSettings
import models.Message
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import services.hardware.HardwareLayer

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject() (
    settings: ApplicationSettings,
    hardware: HardwareLayer,
    actorSystem: ActorSystem,
    lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext)
    extends Logging {
  implicit val system = actorSystem

  private val display = {
    new DisplayService(
      hardware.serialSink,
      settings.signs,
      () => ClockService.calendarSource(settings.signs)
    )
  }

  private val messageScheduler = new MessageSchedulingService(
    hardware.pressedSource,
    hardware.indicatorSink
  )

  messageScheduler.requestSource
    .to(Sink.foreach { msg =>
      display.start(
        MessageService.messageSource(settings.signs, msg.sender, msg.text)
      )
    })
    .run()

  def clock(): Unit = {
    display.start(ClockService.calendarSource(settings.signs))
  }

  def gameOfLife(): Unit = {
    display.start(GameOfLifeService.source(settings.signs))
  }

  def message(sender: String, text: String) =
    messageScheduler.message(Message(sender, text))

  lifecycle.addStopHook({
    // Wrap up when we're done
    () => Future.successful(display.stop())
  })
}
