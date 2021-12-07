package services

import akka.actor.ActorSystem
import play.api.Logging
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject() (lifecycle: ApplicationLifecycle) extends Logging {
  private val size = (84, 7)
  private val signs = Seq(
    "top" -> (2, size),
    "bottom" -> (1, size),
  )
  private val signsNoAddress = signs.map({ case (name, (address, size)) => (name, size) })

  private val display = {
    implicit val system: ActorSystem = ActorSystem("flipsys")

    val sink = { () => SignsService.signsSink("dev/tty.usbserial-0001", signs.toMap) }
    val sources = Map(
      "gameOfLife" -> { () => GameOfLifeService.source(signsNoAddress) },
      "clock" -> { () => ClockService.calendarSource(signsNoAddress) }
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
