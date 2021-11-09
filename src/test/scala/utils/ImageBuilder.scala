package utils

import data.Image

object ImageBuilder {
  def fromStringArt(text: String): Image = {
    val rows = text.split("\n").toVector.filter(_.nonEmpty)
    val data = rows.map(
      _.replace("|", "")
        .map({
          case ' ' => false
          case _   => true
        })
        .toVector
    )
    Image(data)
  }
}
