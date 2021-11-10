package services

import akka.NotUsed
import akka.stream.SourceShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import com.github.nscala_time.time.Imports.DateTime
import models.{GameOfLife, Image}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.duration.FiniteDuration

object ClockService {
  def calendarSource(size: (Int, Int)): Source[(String, Image), NotUsed] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[(String, Image)](2))
      val topImages = clockSource(2 seconds)
        .via(timeRenderer(DateTimeFormat.forPattern("HH:mm:ss")))
        .via(textToImageFlow(size))
        .map(("top", _))
      val bottomImages = clockSource(1 day)
        .via(timeRenderer(DateTimeFormat.forPattern("EEE, MMM d")))
        .via(textToImageFlow(size))
        .map(("bottom", _))

      topImages ~> merge
      bottomImages ~> merge

      SourceShape.of(merge.out)
    })
  }

  private def clockSource(interval: FiniteDuration): Source[DateTime, _] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

  private def timeRenderer(format: DateTimeFormatter): Flow[DateTime, String, NotUsed] =
    Flow[DateTime].map(dt => format.print(dt))

  private def textToImageFlow(size: (Int, Int)): Flow[String, Image, NotUsed] =
    Flow[String].map(Image.fromText(size, _))
}
