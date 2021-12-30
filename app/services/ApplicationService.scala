package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import clients.SerializerSink
import config.ApplicationSettings
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject() (
    config: Configuration,
    lifecycle: ApplicationLifecycle
) extends Logging {
  private val conf = config.get[ApplicationSettings]("flipsys")

  private val display = {
    implicit val system: ActorSystem = ActorSystem("flipsys")
    new DisplayService(
      {
        if (!conf.stubPort) SerializerSink(conf.port)
        else Sink.ignore
      },
      conf.signs
    )
  }

  def clock(): Unit = {
    display.start(ClockService.calendarSource(conf.signs))
  }

  def gameOfLife(): Unit = {
    display.start(GameOfLifeService.source(conf.signs))
  }

  def message(sender: String, message: String): Unit = {
    display.start(MessageService.messageSource(conf.signs, sender, message))
  }

  lifecycle.addStopHook({
    // Wrap up when we're done
    () => Future.successful(display.stop())
  })

  clock()
}
