package services

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.{Flow, Source}
import com.github.nscala_time.time.Imports.DateTime
import config.SignConfig
import models.{Frame, Image}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.Logging
import services.StreamTypes.DisplayPayload

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.duration.FiniteDuration

object ClockService extends Logging {
  def calendarSource(signs: Seq[SignConfig]): Source[Frame, Cancellable] = {
    require(signs.size == 2)
    dateSource(signs(1))
      .merge(clockSource(signs.head))
      .map(Seq(_))
      .conflate { (first, second) =>
        // Combine, overwriting first with second
        val out = (first.toMap ++ second.toMap).toSeq
        logger.info(s"$first & $second to $out")
        out
      }
      .map(Frame)
  }

  private def clockSource(
      sign: SignConfig
  ): Source[DisplayPayload, Cancellable] =
    tickSource(2 seconds)
      .via(timeRenderer(DateTimeFormat.forPattern("HH:mm:ss")))
      .via(textToImageFlow(sign.size))
      .map((sign.name, _))

  private def dateSource(
      sign: SignConfig
  ): Source[DisplayPayload, Cancellable] =
    tickSource(1 day)
      .via(timeRenderer(DateTimeFormat.forPattern("EEE, MMM d")))
      .via(textToImageFlow(sign.size))
      .map((sign.name, _))

  private def tickSource(
      interval: FiniteDuration
  ): Source[DateTime, Cancellable] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

  private def timeRenderer(
      format: DateTimeFormatter
  ): Flow[DateTime, String, NotUsed] =
    Flow[DateTime].map(dt => format.print(dt))

  private def textToImageFlow(
      size: (Int, Int)
  ): Flow[String, Image, NotUsed] = {
    Flow[String]
      .map(s => Image.frame(size, s.toUpperCase()))
      .collect { case Right(img) =>
        img
      }
  }
}
