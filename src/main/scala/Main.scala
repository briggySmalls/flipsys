import akka.actor.ActorSystem
import services.{ClockService, DisplayService, GameOfLifeService, SignsService}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  implicit val system = ActorSystem("flipsys")

  private val size = (84, 7)
  private val signs = Seq(
    "top" -> (2, size),
    "bottom" -> (1, size),
  )
  private val signsNoAddress = signs.map({case (name, (address, size)) => (name, size)})

  val sink = {() => SignsService.signsSink("dev/tty.usbserial-0001", signs.toMap)}

  val sources = Map(
    "gameOfLife" -> {() => GameOfLifeService.source(signsNoAddress)},
    "clock" -> {() => ClockService.calendarSource(signsNoAddress)}
  )

  val display = new DisplayService(sources, sink)
  display.start("clock")

  system.scheduler.scheduleOnce(30 seconds){
    display.start("gameOfLife")
  }
}
