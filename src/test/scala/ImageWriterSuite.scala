class ImageWriterSuite  extends munit.FunSuite {
  test("no payload packet bytes") {
    val iw = new ImageWriter((84, 7))
    val image = iw.textToImage("Hello")
    println(image)
  }
}
