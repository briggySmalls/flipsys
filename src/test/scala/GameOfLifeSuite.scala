import models.Image
import org.scalatest._
import flatspec._
import matchers.should._
import org.scalatest.prop.TableDrivenPropertyChecks._
import services.GameOfLife
import utils.ImageBuilder

import scala.annotation.tailrec

class GameOfLifeSuite  extends AnyFlatSpec with Matchers {
  val block = ImageBuilder.fromStringArt(
    """
      |    |
      | ** |
      | ** |
      |    |
      |""".stripMargin)

  val tub = ImageBuilder.fromStringArt(
    """
      |     |
      |  *  |
      | * * |
      |  *  |
      |     |
      |""".stripMargin)

  val stillLives = Table(
      ("stillLife"),
      (block),
      (tub)
    )

  "A still life" should "never change" in {
    forAll(stillLives) { stillLife =>
      val iterations = getIterations(GameOfLife(stillLife), 2)
      iterations should be (for {_ <- 0 until 2} yield stillLife)
    }
  }

  val blinker = Seq(
    ImageBuilder.fromStringArt(
      """
        |     |
        |  *  |
        |  *  |
        |  *  |
        |     |
        |""".stripMargin),
    ImageBuilder.fromStringArt(
      """
        |     |
        |     |
        | *** |
        |     |
        |     |
        |""".stripMargin),
  )

  val oscillators = Table(
    ("oscillator"),
    (blinker),
  )

  "An oscillator" should "repeat" in {
    forAll(oscillators) { oscillator =>
      val iterations = getIterations(services.GameOfLife(oscillator(0)), 5)
      iterations should equal ((for(_ <- 0 until 3; frame <- oscillator) yield frame).tail)
    }
  }

  "A game" should "play" in {
    var gol = services.GameOfLife(blinker(0))
    for (_ <- 0 to 5) {
      println(gol.image)
      gol = gol.iterate()
    }
  }


  private def getIterations(gol: GameOfLife, iterations: Int): Seq[Image] = {
    @tailrec
    def _getIterations(gol: GameOfLife, acc: Seq[Image], iterations: Int): Seq[Image] = {
      if (iterations == 0) acc
      else _getIterations(gol.iterate(), acc :+ gol.image, iterations - 1)
    }
    _getIterations(gol.iterate(), Seq[Image](), iterations)
  }

//  private def getIterations(gol: services.GameOfLife, iterations: Int): Seq[Image] = {
//    var internalGol = gol
//    for {
//      _ <- 0 until iterations
//    } yield {
//      internalGol = internalGol.iterate()
//      internalGol.image
//    }
//  }
}
