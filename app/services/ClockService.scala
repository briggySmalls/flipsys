package services

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.SourceShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import com.github.nscala_time.time.Imports.DateTime
import config.SignConfig
import models.{GameOfLife, Image}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.duration.FiniteDuration

object ClockService {
  def calendarSource(signs: Seq[SignConfig]): Source[(String, Image), NotUsed] = {
    require(signs.size == 2)

    Source.combine(
      clockSource(2 seconds)
        .via(timeRenderer(DateTimeFormat.forPattern("HH:mm:ss")))
        .via(textToImageFlow(signs.head.size))
        .map((signs.head.name, _)),
      clockSource(1 day)
        .via(timeRenderer(DateTimeFormat.forPattern("EEE, MMM d")))
        .via(textToImageFlow(signs(1).size))
        .map((signs(1).name, _))
    )(Merge(_))
  }

  private def clockSource(interval: FiniteDuration): Source[DateTime, Cancellable] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

  private def timeRenderer(format: DateTimeFormatter): Flow[DateTime, String, NotUsed] =
    Flow[DateTime].map(dt => format.print(dt))

  private def textToImageFlow(size: (Int, Int)): Flow[String, Image, NotUsed] =
    Flow[String].map(s => Image.fromText(size, s.toUpperCase()))
}
