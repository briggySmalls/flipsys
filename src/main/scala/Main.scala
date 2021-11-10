import akka.actor.ActorSystem
import akka.stream.scaladsl.RunnableGraph
import services.{DisplayService, GameOfLifeService, SignsService}

object Main extends App {
  implicit val system = ActorSystem("flipsys")

  private val size = (84, 7)
  private val signs = Seq(
    "top" -> (2, size),
    "bottom" -> (1, size),
  )

  val sink = SignsService.signsSink("dev/tty.usbserial-0001", signs.toMap)

  val sources = Map(
    "gameOfLife" -> GameOfLifeService.source(signs.map({case (name, (address, size)) => (name, size)}))
  )

  val display = new DisplayService(sources, sink)
  display.start("gameOfLife")
}
