package services

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.{Flow, Source}
import models.Image
import services.StreamTypes.DisplayPayload

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object GameOfLifeService {
  def source(signs: Seq[(String, (Int, Int))]): Source[DisplayPayload, Cancellable] = {
    val firstCols = signs.head._2 match {
      case (cols, _) => cols
    }
    val totalSize = signs.foldLeft((firstCols, 0))({
      case ((totalCols, totalRows), (_, (cols, rows))) =>
        require(cols == totalCols)
        (totalCols, totalRows + rows)
    })
    val random = new Random()
    val seed = Image(Vector.tabulate(totalSize._2, totalSize._1)({
        case (_, _) => random.nextBoolean()
    }))
    simpleSource(2 seconds, seed).via(splitImages(signs))
  }

  private def simpleSource(interval: FiniteDuration, seed: Image): Source[Image, Cancellable] =
    Source.tick(0 second, interval, "tick").statefulMapConcat({() =>
      var gol = models.GameOfLife(seed)
      _ =>
          val output = gol.image
          gol = gol.iterate()
          (output :: Nil)
    })

  private def splitImages(signs: Seq[(String, (Int, Int))]): Flow[Image, DisplayPayload, NotUsed] = {
    Flow[Image].mapConcat(image => {
      signs.foldLeft(Seq(("rest", image)))({ case (images, (name, (_, row))) =>
        val (init, (restName, lastImage)) = (images.init, images.last)
        val (split, rest) = lastImage.data.splitAt(row)
        val processed = init :+ (name, Image(split))

        if (rest.isEmpty) processed
        else processed :+ (restName, Image(rest))
      })
    })
  }
}
