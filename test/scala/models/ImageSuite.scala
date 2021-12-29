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

  val frameTestTable: TableFor3[(Int, Int), String, Either[String, Image]] = Table(
      ("size", "text", "expected"),
      ((4, 4), "overflow", Left("Word 'overflow' doesn't fit in a frame")),
      ((25, 7), "too many", Left("Text 'too many' larger than a single frame")),
      ((4, 4), "", Right(
        ImageBuilder.fromStringArt("""
          |    |
          |    |
          |    |
          |    |
          |""".stripMargin
        )
      )),
      ((20, 7), "Hello", Right(
        ImageBuilder.fromStringArt("""
        | *  *      * *      |
        | *  *  *** * *  **  |
        | **** *  * * * *  * |
        | *  * * *  * * *  * |
        | *  *  *** * *  **  |
        |                    |
        |                    |
        |""".stripMargin
        )
      ))
  )

  forAll(frameTestTable) { (size, text, expected) =>
    it should s"create single frame with '$text'" in {
      Image.frame(size, text) should equal (expected)
    }
  }

  val framesTestTable: TableFor3[(Int, Int), String, Either[String, Seq[Image]]] = Table(
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

  forAll(framesTestTable) { (size: (Int, Int), text: String, expected: Either[String, Seq[Image]]) =>
    it should s"create multiple frames for '$text'" in {
      Image.frames(size, text) should equal (expected)
    }
  }
}
