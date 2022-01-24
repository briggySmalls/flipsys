package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import config.ApplicationSettings
import models.Message
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import services.hardware.HardwareLayer

import javax.inject.{Inject, Singleton}
import scala.collection.immutable.Queue
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
  private val activateSource = Source.queue[Unit](16)
  private val (activateSourceQueue, materializedActivateSource) =
    activateSource.preMaterialize()
  private val button = new ButtonService(
    materializedActivateSource,
    hardware.pressedSource,
    hardware.indicatorSink
  )
  private var messages = Queue[Message]()

  button.requestSource
    .to(Sink.foreach { _ =>
      dequeue()
      ()
    })
    .run()

  def clock(): Unit = {
    display.start(ClockService.calendarSource(settings.signs))
  }

  def gameOfLife(): Unit = {
    display.start(GameOfLifeService.source(settings.signs))
  }

  def message(sender: String, text: String): Unit = {
    messages = messages.enqueue(Message(sender, text))
    logger.info("queuing activation")
    activateSourceQueue.offer()
  }

  def dequeue(): Try[Unit] = {
    for {
      (msg, tail) <- Try(messages.dequeue)
    } yield {
      messages = tail
      display.start(
        MessageService.messageSource(settings.signs, msg.sender, msg.text)
      )
    }
  }

  lifecycle.addStopHook({
    // Wrap up when we're done
    () => Future.successful(display.stop())
  })
}
