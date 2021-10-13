import ImageWriter.textToImage

class ImageWriterSuite  extends munit.FunSuite {
  test("no payload packet bytes") {
    val image = textToImage((84, 7), "Hello")
    println(image)
  }
}
