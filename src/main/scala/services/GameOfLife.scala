package services

import models.Image

case class GameOfLife(image: Image) {
  def iterate(): GameOfLife = {
    val newState = Vector.tabulate(image.rows, image.columns) {
      case (row, col) =>
        val count = neighboursCount(row, col)
        val isAlive = image.data(row)(col)
        if (isAlive && (count == 2 || count == 3)) true
        else if (!isAlive && count == 3) true
        else false
    }
    GameOfLife(new Image(newState))
  }

  private def neighboursCount(row: Int, col: Int): Int =
    (for {
      drow <- row - 1 to row + 1 if (drow >= 0) && (drow < image.rows)
      dcol <- col - 1 to col + 1 if (dcol >= 0) && (dcol < image.columns)
    } yield {
      if (drow == row && dcol == col) 0
      else if (image.data(drow)(dcol)) 1
      else 0
    }).sum
}
