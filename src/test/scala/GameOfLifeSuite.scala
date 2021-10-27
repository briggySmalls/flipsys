import data.Image
import org.scalatest._
import flatspec._
import matchers.should._
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.MatchResult

class GameOfLifeSuite  extends AnyFlatSpec with Matchers {
  val block = Vector.tabulate(4, 4)({
    case (x, y) if (x >= 1) && (x <= 2) && (y >= 1) && (y <= 2) => true
    case _ => false
  })

  val tub = Vector.tabulate(5, 5)({
    case (x, y) if (y == 2) && ((x == 1) || (x == 3)) => true
    case (x, y) if (x == 2) && ((y == 1) || (y == 3)) => true
    case _ => false
  })

  val stillLives = Seq(
    block,
    tub
  )

  private def testIterations(gol: GameOfLife, expected: Seq[Vector[Vector[Boolean]]]): Assertion = {
    expected match {
      case Nil => true should be (true)
      case next :: rest =>
        gol.image.image should be (next)
        testIterations(gol.iterate(), rest)
    }
  }

  "A still life" should "never change" in {
    forAll(stillLives) { stillLife =>
      testIterations(
        GameOfLife(new Image(stillLife)),
        for (_ <- 0 to 2) yield stillLife
      )
    }
  }

  val blinker = Seq(
    Vector.tabulate(5, 5)({
      case (x, y) if (x == 2) && (y > 0) && (y < 4) => true
      case _ => false
    }),
    Vector.tabulate(5, 5)({
      case (x, y) if (y == 2) && ((x != 0) && (x != 4)) => true
      case _ => false
    })
  )

  val oscillators = Seq(blinker)

  "An oscillator" should "repeat" in {
    forAll(oscillators) { oscillator =>
      testIterations(
        GameOfLife(new Image(oscillator(0))),
        (for (_ <- 0 to 5; frame <- oscillator) yield frame).tail
      )
    }
  }

  "A game" should "play" in {
    var gol = GameOfLife(new Image(blinker(0)))
    for (_ <- 0 to 5) {
      println(gol.image)
      gol = gol.iterate()
    }
  }
}
