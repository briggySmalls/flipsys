package data

object Image {}

class Image(val image: Vector[Vector[Boolean]]) {
  require(image.length > 0)
  require({
    val lengths = image.map(_.length)
    lengths.forall(_ == lengths.head)
  })

  def rows = image.length
  def columns = image(0).length
}
