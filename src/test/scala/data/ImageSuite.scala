package data

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.ImageBuilder

class ImageSuite extends AnyFlatSpec with Matchers {
  "An image" should "be rotatable" in {
    val testImage = ImageBuilder.fromStringArt(
      """
        |***|
        |   |
        |""".stripMargin)

    val rotated = testImage.rotate90()
    rotated should equal (ImageBuilder.fromStringArt(
      """
        | *|
        | *|
        | *|
        |""".stripMargin))
  }
}
