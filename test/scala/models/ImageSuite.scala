package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import org.scalatest.prop.TableDrivenPropertyChecks.Table
import utils.ImageBuilder

class ImageSuite extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {
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

  val frameTestTable: TableFor3[(Int, Int), String, Either[String, Seq[Image]]] = Table(
      ("size", "text", "expected"),
      ((20, 7), "Hellooo world", Left("Word 'Hellooo' doesn't fit in a frame")),
      ((20, 7), "Hello world", Left("Word 'world' doesn't fit in a frame")),
      ((20, 7), "Hello dolly", Right(Seq(
        ImageBuilder.fromStringArt("""
          | *  *      * *      |
          | *  *  *** * *  **  |
          | **** *  * * * *  * |
          | *  * * *  * * *  * |
          | *  *  *** * *  **  |
          |                    |
          |                    |
          |""".stripMargin
        ),
        ImageBuilder.fromStringArt("""
          |    *      * *      |
          |  ***  **  * * *  * |
          | *  * *  * * * *  * |
          | *  * *  * * * *  * |
          |  ***  **  * *  *** |
          |                  * |
          |                **  |""".stripMargin
        )
      ))),
    )

  forAll(frameTestTable) { (size: (Int, Int), text: String, expected: Either[String, Seq[Image]]) =>
    it should s"create frames for text '$text'" in {
      Image.frames(size, text) should equal (expected)
    }
  }
}
