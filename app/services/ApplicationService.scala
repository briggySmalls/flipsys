package services

import akka.actor.ActorSystem
import config.ApplicationSettings
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject() (config: Configuration, lifecycle: ApplicationLifecycle) extends Logging {
  private val conf = config.get[ApplicationSettings]("flipsys")

  private val display = {
    implicit val system: ActorSystem = ActorSystem("flipsys")

    val sink = SignsService.signsSink(conf.port, conf.signs)
    val sources = Map(
      "gameOfLife" -> { () => GameOfLifeService.source(conf.signs) },
      "clock" -> { () => ClockService.calendarSource(conf.signs) }
    )
    new DisplayService(sources, sink)
  }

  def start(source: String): Either[String, Unit] = {
    logger.info(s"Starting source $source")
    display.start(source)
  }

  lifecycle.addStopHook({
    // Wrap up when we're done
    () => Future.successful(display.stop())
  })

  start("clock")
}
