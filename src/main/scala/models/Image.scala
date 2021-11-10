package models

import java.awt.{Color, Font}
import java.awt.image.BufferedImage

case class Image(val data: Vector[Vector[Boolean]]) {
  require(data.length > 0)
  require({
    val lengths = data.map(_.length)
    lengths.forall(_ == lengths.head)
  })

  def rows = data.length
  def columns = data(0).length

  def transpose(): Image = {
    Image(data.transpose)
  }

  def reverseRows(): Image = {
    Image(data.map(_.reverse))
  }

  def rotate90(): Image = {
    transpose().reverseRows()
  }

  override def toString(): String = {
    s"\n${data.map(row =>
      s"|${row.map(if (_) "*" else " ").mkString}|\n"
    ).mkString}"
  }
}

object Image {
  def fromText(size: (Int, Int), text: String): Image = {
    // Create a new, blank image
    val newImage =
      new BufferedImage(size._1, size._2, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = newImage.createGraphics()
    graphics.setColor(Color.BLACK)
    graphics.fillRect(0, 0, size._1, size._2)
    //Font settings
    graphics.setFont(
      Font
        .createFont(
          Font.TRUETYPE_FONT,
          getClass.getResourceAsStream("Smirnof.ttf")
        )
        .deriveFont(8f)
    )
    //Add characters
    graphics.setColor(Color.WHITE)
    graphics.drawString(text, 0, 5)
    val first = newImage.getRGB(1, 0)
    Image(
      Vector.tabulate(size._2, size._1)((y, x) =>
        new Color(newImage.getRGB(x, y)) == Color.WHITE
      )
    )
  }
}