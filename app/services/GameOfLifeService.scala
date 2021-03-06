package services

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import config.SignConfig
import models.{Frame, Image}
import services.StreamTypes.DisplayPayload

import scala.util.Random

object GameOfLifeService {
  def source(signs: Seq[SignConfig]): Source[Frame, NotUsed] = {
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
    Source
      .unfold(models.GameOfLife(seed)) { current =>
        Some(current.iterate(), current.image)
      }
      .via(splitImages(signs))
      .map(Frame)
  }

  private def splitImages(
      signs: Seq[SignConfig]
  ): Flow[Image, Seq[DisplayPayload], _] = {
    Flow[Image].map(image => {
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
