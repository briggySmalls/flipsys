package data

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ImageSuite extends AnyFlatSpec with Matchers {
  "An image" should "be rotatable" in {
    val testImage = toImage(
      """
        |***|
        |   |
        |""".stripMargin)

    val rotated = testImage.rotate90()
    rotated should equal (toImage(
      """
        | *|
        | *|
        | *|
        |""".stripMargin))
  }

  private def toImage(text: String): Image = {
    val rows = text.split("\n").toVector.filter(_.nonEmpty)
    val data = rows.map(_.replace("|", "").map({
      case ' ' => false
      case _ => true
    }).toVector)
    Image(data)
  }
}
