package services

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.{Flow, Source}
import config.SignConfig
import models.Image
import services.StreamTypes.DisplayPayload

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object GameOfLifeService {
  def source(signs: Seq[SignConfig]): Source[DisplayPayload, NotUsed] = {
    val sizes = signs.map(_.size)
    // Ensure images are vertically stackable
    require(sizes.map(_._1).forall(_ == sizes.head._1))
    val totalSize = sizes.foldLeft((sizes.head._1, 0))({
      case ((totalCols, totalRows), (cols, rows)) =>
        require(cols == totalCols)
        (totalCols, totalRows + rows)
    })
    val random = new Random()
    val seed = Image(Vector.tabulate(totalSize._2, totalSize._1)({
        case (_, _) => random.nextBoolean()
    }))
    simpleSource(2 seconds, seed).via(splitImages(signs))
  }

  private def simpleSource(interval: FiniteDuration, seed: Image): Source[Image, NotUsed] = {
    Source.unfold(models.GameOfLife(seed)) { current =>
      Some(current.iterate(), current.image)
    }
  }

  private def splitImages(signs: Seq[SignConfig]): Flow[Image, DisplayPayload, NotUsed] = {
    Flow[Image].mapConcat(image => {
      signs.foldLeft(Seq(("rest", image)))({ case (images, config) =>
        val (init, (restName, lastImage)) = (images.init, images.last)
        val (split, rest) = lastImage.data.splitAt(config.size._2)
        val processed = init :+ (config.name, Image(split))

        if (rest.isEmpty) processed
        else processed :+ (restName, Image(rest))
      })
    })
  }
}
