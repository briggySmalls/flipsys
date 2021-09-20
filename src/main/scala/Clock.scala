import akka.actor.{Actor, ActorLogging, Props}
import data.Image

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object Clock {
  object Tick
  case class TimeImage(val image: Image)

  def props(size: (Int, Int), interval: FiniteDuration) = Props(new Clock(size, interval))
}

class Clock(val size: (Int, Int), interval: FiniteDuration) extends Actor with ActorLogging {
  import Clock._
  import context.dispatcher

  private val random = new Random()

  context.system.scheduler.scheduleAtFixedRate(0 seconds, interval, self, Tick)

  override def receive: Receive = {
    case Tick => context.parent ! TimeImage(generateImage())
  }

  private def generateImage() = new Image(Vector.tabulate(size._2, size._1)((_, _) => random.nextBoolean()))
}
