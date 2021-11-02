import data.Image
import org.scalatest._
import flatspec._
import matchers.should._
import org.scalatest.prop.TableDrivenPropertyChecks._

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

  private def getIterations(gol: GameOfLife, iterations: Int): Seq[Image] = {
    var internalGol = gol
    for {
      _ <- 0 until iterations
    } yield {
      internalGol = internalGol.iterate()
      gol.image
    }
  }

//  "A still life" should "never change" {
//    val stillLives = Table(
//      ("stillLife"),
//      (block),
//      (tub)
//    )
//    forAll(stillLives) { stillLife =>
//      val iterations = getIterations(GameOfLife(new Image(stillLife)), 2)
//      iterations.foreach(image => image should equal (stillLife))
//    }
//  }

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

  val oscillators = Table(
    ("oscillator"),
    (blinker),
  )

  "An oscillator" should "repeat" in {
    forAll(oscillators) { oscillator =>
      val iterations = getIterations(GameOfLife(new Image(oscillator(0))), 5)
      val expected = (for(_ <- 0 until 5; frame <- oscillator) yield frame).tail
      iterations.zip(expected).foreach {
        case (actual, expected) => actual should equal (expected)
      }
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
