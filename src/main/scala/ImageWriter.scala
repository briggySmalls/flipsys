import data.Image

import java.awt.{Color, Font}
import java.awt.image.BufferedImage

object ImageWriter {
  def textToImage(size: (Int, Int), text: String): Image = {
    // Create a new, blank image
    val newImage = new BufferedImage(size._1, size._2, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = newImage.createGraphics()
    graphics.setColor(Color.BLACK)
    graphics.fillRect(0, 0, size._1, size._2)
    //Font settings
    graphics.setFont(
      Font.createFont(
        Font.TRUETYPE_FONT, getClass.getResourceAsStream("Smirnof.ttf")
      ).deriveFont(8f)
    )
    //Add characters
    graphics.setColor(Color.WHITE)
    graphics.drawString(text, 0, 5)
    val first = newImage.getRGB(1, 0)
    new Image(Vector.tabulate(size._2, size._1)((y, x) =>
      new Color(newImage.getRGB(x, y)) == Color.WHITE
    ))
  }
}
