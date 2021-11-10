package models

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

  "Image object" should "create empty image" in {
    Image.fromText((4, 4), "") should equal (ImageBuilder.fromStringArt(
      """
        |    |
        |    |
        |    |
        |    |
        |""".stripMargin
    ))
  }

  it should "create simple text" in {
    Image.fromText((20, 7), "Hello") should equal (ImageBuilder.fromStringArt(
      """
        | *  *      * *      |
        | *  *  *** * *  **  |
        | **** *  * * * *  * |
        | *  * * *  * * *  * |
        | *  *  *** * *  **  |
        |                    |
        |                    |
        |""".stripMargin
    ))
  }
}
