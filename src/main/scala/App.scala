import ImageWriter.textToImage
import akka.NotUsed
import akka.stream.{ClosedShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source, Zip}
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat}
import data.Image
import data.Packet.DrawImage
import org.joda.time.format.DateTimeFormatter

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object App {
  private val size = (84, 7)

  val graph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._ // brings some nice operators in scope

    val random = new Random()
    val source = bigGameOfLifeSource(2 seconds, new Image(Vector.tabulate(size._2 * 2, size._1)({
        case (_, _) => random.nextBoolean()
    })))

    val signs = Map(
      "top" -> (1, size),
      "bottom" -> (2, size),
    )
    val sink = signsSink("dev/tty.usbserial-0001", signs)

    // Wire it up
    source ~> sink

    ClosedShape
  }

  def calendarSource(): Source[(String, Image), NotUsed] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[(String, Image)](2))
      val topImages = clockSource(2 seconds)
        .via(timeRenderer(DateTimeFormat.forPattern("HH:mm:ss")))
        .via(textToImageFlow(size))
        .map(("top", _))
      val bottomImages = clockSource(1 day)
        .via(timeRenderer(DateTimeFormat.forPattern("EEE, MMM d")))
        .via(textToImageFlow(size))
        .map(("bottom", _))

      topImages ~> merge
      bottomImages ~> merge

      SourceShape.of(merge.out)
    })
  }

  def bigGameOfLifeSource(interval: FiniteDuration, seed: Image): Source[(String, Image), _] =
    splitImages(Source.tick(0 second, interval, "tick").statefulMapConcat({() =>
      var gol = GameOfLife(seed)
      _ =>
          val output = gol.image
          gol = gol.iterate()
          (output :: Nil)
    }))

  def clockImageSource(size: (Int, Int)) =
    clockSource(2 seconds)
      .via(timeRenderer(DateTimeFormat.forPattern("HH:mm:ss")))
      .via(textToImageFlow(size))

  def clockSource(interval: FiniteDuration): Source[DateTime, _] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

  def gameOfLifeSource(interval: FiniteDuration, seed: Image): Source[Image, _] = {
    Source.tick(0 second, interval, "tick").statefulMapConcat({() =>
      var gol = GameOfLife(seed)

      _ =>
          val output = gol.image
          gol = gol.iterate()
          (output :: Nil)
    })
  }

  def signsSink(serialPort: String, signs: Map[String, (Int, (Int, Int))]): Sink[(String, Image), NotUsed] =
    Sink.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sink = new SerializerSink(serialPort)
      val broadcast = builder.add(Broadcast[(String, Image)](signs.size))
      val signFlows = signs.map({
        case (id, (address, size)) => builder.add(
          Flow[(String, Image)]
            .filter(_._1 == id)
            .map(_._2)
            .log(id, _.toString())
            .via(signFlow(address, size))
        )
      })
      val merge = builder.add(Merge[Seq[Byte]](signs.size))
      val flip = builder.add(Flow[(String, Image)].map({case (id, image) =>
        if (id == "top")
          (id, image.rotate90().rotate90())
        else
          (id, image)
      }))

      flip ~> broadcast
      signFlows.foreach( flow => {
          broadcast ~> flow ~> merge
      })
      merge ~> sink

      SinkShape.of(flip.in)
    })

  def timeRenderer(format: DateTimeFormatter): Flow[DateTime, String, NotUsed] =
    Flow[DateTime].map(dt => format.print(dt))

  def textToImageFlow(size: (Int, Int)): Flow[String, Image, NotUsed] =
    Flow[String].map(textToImage(size, _))

  def rotate180Flow(): Flow[Image, Image, NotUsed] =
    Flow[Image].map(_.rotate90().rotate90())

  def signFlow(address: Int, size: (Int, Int)): Flow[Image, Seq[Byte], NotUsed] =
    Flow[Image].map(image => size match {
        case (width, _) if (width != image.columns) => throw new Error(s"Incompatible image width (${image.columns}, should be ${width})")
        case (_, height) if (height != image.rows) => throw new Error(s"Incompatible image width (${image.rows}, should be ${height})")
        case _ => {
          DrawImage(address, image).bytes
        }
      })

  def splitImages(source: Source[Image, _]) =
    source.flatMapConcat(image => {
      val (top, bottom) = image.data.splitAt(size._2)
      Source(List(
        ("top", new Image(top)),
        ("bottom", new Image(bottom)),
      ))
    })
}
