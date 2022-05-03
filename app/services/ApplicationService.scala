package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink}
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

  private val messageScheduler = new MessageSchedulingService()

  // Start a stream for latching messages into the display service
  RunnableGraph
    .fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val scheduler = builder.add(messageScheduler.graph)
        val pressed = builder.add(hardware.pressedSource)
        val indicator = builder.add(hardware.indicatorSink)
        val sender = builder.add(Sink.foreach { msg: Message =>
          display.start(
            MessageService.messageSource(settings.signs, msg.sender, msg.text)
          )
        })

        pressed ~> scheduler.pressed
        scheduler.indicator ~> indicator
        scheduler.message ~> sender

        ClosedShape
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
