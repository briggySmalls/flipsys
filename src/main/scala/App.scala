import akka.NotUsed
import akka.stream.{ClosedShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source, Zip}
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat}
import data.Image
import data.Packet.DrawImage

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object App {
  private val size = (84, 7)
  private val imageWriter = new ImageWriter(size)

  val graph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._ // brings some nice operators in scope

    val signs = Map(
      "top" -> (1, size),
      "bottom" -> (2, size),
    )
    val merge = builder.add(Merge[(String, Image)](signs.size))
    val sink = signsSink("dev/tty.usbserial-0001", signs)

    val topImages = clockImageSource(size).map(("top", _))
    val bottomImages = clockImageSource(size).map(("bottom", _))

    topImages ~> merge
    bottomImages ~> merge
    merge ~> sink

    // Indicate the graph is over
    ClosedShape
  }

  def clockImageSource(size: (Int, Int)) =
    clockSource(2 seconds).via(clockImageFlow(size))

  def clockSource(interval: FiniteDuration): Source[DateTime, _] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

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
            .log(id, drawImage(_))
            .via(signFlow(address, size))
        )
      })
      val merge = builder.add(Merge[Seq[Byte]](signs.size))

      signFlows.foreach(flow => {
        broadcast ~> flow ~> merge
      })
      merge ~> sink

      SinkShape.of(broadcast.in)
    })

  def clockImageFlow(size: (Int, Int)): Flow[DateTime, Image, NotUsed] = {
    val fmt = DateTimeFormat.forPattern("HH:mm:ss");
    Flow[DateTime].map(datetime => imageWriter.textToImage(fmt.print(datetime)))
  }

  def signFlow(address: Int, size: (Int, Int)): Flow[Image, Seq[Byte], NotUsed] =
    Flow[Image].map(image => size match {
        case (width, _) if (width != image.columns) => throw new Error(s"Incompatible image width (${image.columns}, should be ${width})")
        case (_, height) if (height != image.rows) => throw new Error(s"Incompatible image width (${image.rows}, should be ${height})")
        case _ => {
          DrawImage(address, image).bytes
        }
      })

  def drawImage(image: Image): String =
    "\n" + (
      for {
        row <- 0 until image.rows
        col <- 0 until image.columns
      }
        yield (image.image(row)(col) match {
          case true => "*"
          case false => " "
        }) + (if (col == image.columns - 1) "\n" else "")
    ).mkString("")
}
