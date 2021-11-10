package models

import java.awt.font.FontRenderContext
import java.awt.{Color, Font}
import java.awt.image.BufferedImage
import java.io.InputStream

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
  val font = getFont()

  def getFont(): Font =
  {
    val stream = getClass.getResourceAsStream("/Smirnof.ttf")
    require(stream != null)
    Font.createFont(
      Font.TRUETYPE_FONT,
      stream
    ).deriveFont(8f)
  }


  def fromText(size: (Int, Int), text: String): Image = {
    val (width, height) = size
    // Create a new, blank image
    val newImage =
      new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = newImage.createGraphics()
    graphics.setColor(Color.BLACK)
    graphics.fillRect(0, 0, width, height)
    //Font settings
    graphics.setFont(font)
    //Add characters
    graphics.setColor(Color.WHITE)
    val frc = new FontRenderContext(null, false, false)
    val metrics = font.getLineMetrics(text, frc)
    val bounds = font.getStringBounds(text, frc)
    graphics.drawString(
      text,
      math.round((width - bounds.getWidth) / 2),
      math.round(math.max((height - bounds.getHeight) / 2, 0) + metrics.getAscent))


    Image(
      Vector.tabulate(size._2, size._1)((y, x) =>
        new Color(newImage.getRGB(x, y)) == Color.WHITE
      )
    )
  }
}