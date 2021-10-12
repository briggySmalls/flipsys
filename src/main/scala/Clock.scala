import akka.NotUsed
import com.github.nscala_time.time.Imports._
import akka.stream.scaladsl.{Flow, Source}
import data.Image

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object Clock {
  def clockSource(interval: FiniteDuration): Source[DateTime, _] =
    Source.tick(0 second, interval, "tick").map(
      DateTime.now()
    )
}

class Clock {
  private val random = new Random()

  def clockImageFlow(size: (Int, Int)): Flow[DateTime, Image, NotUsed] =
    Flow[DateTime, Image, NotUsed].map(
      new Image(Vector.tabulate(size._2, size._1)((_, _) => random.nextBoolean()))
    )
}
