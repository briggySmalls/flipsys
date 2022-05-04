package services

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.SourceShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
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
  def calendarSource(signs: Seq[SignConfig]): Source[Frame, NotUsed] = {
    require(signs.size == 2)
    Source.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val clock = clockSource(signs.head)
        val date = dateSource(signs(1))

        val merge = builder.add(Merge[DisplayPayload](2))
        val conflate = builder.add(
          Flow[DisplayPayload]
            .map(Map(_))
            .conflate { (first, second) =>
              first ++ second // Combine, overwriting first with second
            }
            .map(m => Frame(m.toSeq))
        )

        date ~> merge
        clock ~> merge
        merge.out ~> conflate

        SourceShape(conflate.out)
    })
  }

  private def clockSource(
      sign: SignConfig
  ): Source[DisplayPayload, Cancellable] =
    tickSource(1 seconds)
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
