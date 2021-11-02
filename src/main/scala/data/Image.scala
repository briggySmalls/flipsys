package data

object Image {
}

case class Image(val data: Vector[Vector[Boolean]]) {
  require(data.length > 0)
  require({
    val lengths = data.map(_.length)
    lengths.forall(_ == lengths.head)
  })

  def rows = data.length
  def columns = data(0).length

  def transpose(): Image = {
    Image(data.transpose)
  }

  def reverseRows(): Image = {
    Image(data.map(_.reverse))
  }

  def rotate90(): Image = {
    transpose().reverseRows()
  }

  override def toString(): String = {
    s"\n${data.map(row =>
      s"|${row.map(if (_) "*" else " ").mkString}|\n"
    ).mkString}"
  }
}
