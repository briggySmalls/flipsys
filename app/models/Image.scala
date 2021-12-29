package models

import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D
import java.awt.{Color, Font}
import java.awt.image.BufferedImage
import java.io.InputStream
import scala.annotation.tailrec

case class Image(val data: Vector[Vector[Boolean]]) {
  require(data.nonEmpty)
  require({
    val lengths = data.map(_.length)
    lengths.forall(_ == lengths.head)
  })

  def rows: Int = data.length
  def columns: Int = data(0).length

  def transpose(): Image = {
    Image(data.transpose)
  }

  def reverseRows(): Image = {
    Image(data.map(_.reverse))
  }

  def rotate90(): Image = {
    transpose().reverseRows()
  }

  override def toString: String = {
    s"\n${data.map(row =>
      s"|${row.map(if (_) "*" else " ").mkString}|\n"
    ).mkString}"
  }
}

object Image {
  private val font: Font =
  {
    val stream = getClass.getResourceAsStream("/Smirnof.ttf")
    require(stream != null)
    Font.createFont(
      Font.TRUETYPE_FONT,
      stream
    ).deriveFont(8f)
  }

  def frame(size: (Int, Int), text: String): Either[String, Image] =
    frames(size, text).flatMap {
      case head +: Nil => Right(head)
      case _ => Left(s"Text '$text' larger than a single frame")
    }

  def frames(size: (Int, Int), text: String): Either[String, Seq[Image]] = {
    splitToFrames(size._1, text).map(_.map(frameToImage(size, _)))
  }

  private def splitToFrames(width: Int, text: String): Either[String, Seq[String]] = {
    // Helper function for determining if text fits in a frame
    def testIfFits(text: String): Boolean = getTextBounds(text).getWidth < width

    @tailrec
    def _splitToFrames(words: Seq[String], frames: Seq[String], pending: String): Either[String, Seq[String]] = {
      words match {
        case Nil =>
          if (testIfFits(pending)) Right(frames :+ pending)
          else Left(s"Word '$pending' doesn't fit in a frame")
        case s +: tail =>
          val next = s"$pending $s".trim
          if (testIfFits(next)) _splitToFrames(tail, frames, next)  // Still not reached end of the frame
          else if (next == s) Left(s"Word '$s' doesn't fit in a frame")
          else _splitToFrames(tail, frames :+ pending, s) // Word $s was 1 word too many
      }
    }
    _splitToFrames(text.split(' ').toSeq, Seq[String](), "")
  }

  private def frameToImage(size: (Int, Int), text: String): Image = {
    val (width, height) = size
    // Create a new, blank image
    val newImage =
      new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = newImage.createGraphics()
    graphics.setColor(Color.BLACK)
    graphics.fillRect(0, 0, width, height)
    //Font settings
    graphics.setFont(font)
    graphics.setColor(Color.WHITE)
    val metrics = graphics.getFontMetrics()
    //Add characters
    val bounds = getTextBounds(text)
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

  private def getTextBounds(text: String): Rectangle2D = {
    val frc = new FontRenderContext(null, false, false)
    font.getStringBounds(text, frc)
  }
}
