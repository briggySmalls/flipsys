package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph}
import akka.stream.{ClosedShape, KillSwitches, UniqueKillSwitch}
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
      settings.signs,
      () => ClockService.calendarSource(settings.signs)
    )
  }

  private val messageScheduler = new MessageSchedulingService()

  // Start a stream for latching messages into the display service
  val ks: UniqueKillSwitch = RunnableGraph
    .fromGraph(GraphDSL.create(KillSwitches.single[Seq[Byte]]) {
      implicit builder: GraphDSL.Builder[UniqueKillSwitch] => sw =>
        import GraphDSL.Implicits._

        val scheduler = builder.add(messageScheduler.graph)
        val pressed = builder.add(hardware.pressedSource)
        val indicator = builder.add(hardware.indicatorSink)
        val toSource = builder.add(
          Flow[Message].map(msg =>
            MessageService.messageSource(settings.signs, msg.sender, msg.text)
          )
        )
        val displayFlow = builder.add(display.flow)

        // Configure the controls
        pressed ~> scheduler.pressed
        scheduler.indicator ~> indicator

        // Render messages on the signs
        scheduler.message ~> toSource ~> displayFlow ~> sw ~> hardware.serialSink

        ClosedShape
    })
    .run()

  def message(sender: String, text: String) =
    messageScheduler.message(Message(sender, text))

  lifecycle.addStopHook({
    // Wrap up when we're done
    () => Future.successful(ks.shutdown())
  })
}
