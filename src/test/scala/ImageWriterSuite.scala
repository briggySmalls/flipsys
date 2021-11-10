import services.ImageWriter.textToImage
import models.Image
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.ImageBuilder

class ImageWriterSuite extends AnyFlatSpec with Matchers {
  "ImageWriter" should "create empty image" in {
    textToImage((4, 4), "") should equal (ImageBuilder.fromStringArt(
      """
        |    |
        |    |
        |    |
        |    |
        |""".stripMargin
    ))
  }

  it should "create simple text" in {
    textToImage((20, 7), "Hello") should equal (ImageBuilder.fromStringArt(
      """
        |*  *      * *       |
        |*  *  *** * *  **   |
        |**** *  * * * *  *  |
        |*  * * *  * * *  *  |
        |*  *  *** * *  **   |
        |                    |
        |                    |
        |""".stripMargin
    ))
  }
}
