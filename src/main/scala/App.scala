import akka.NotUsed
import akka.stream.ClosedShape
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
  val fmt = DateTimeFormat.forPattern("HH:mm:ss");

  val graph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._ // brings some nice operators in scope

    // Create the elements
    val clockSrc = clockSource(2 seconds)
    val out = new SerializerSink("dev/tty.usbserial-0001")
//    val out = Sink.ignore

    val signAddresses = Seq(1, 2)
    val clockImageFlows = signAddresses.map(
        address => builder.add(
          clockImageFlow(size).log("image", drawImage(_)).via(signFlow(address, size))
        )
    )
    val identity = Flow[Seq[Byte]]
//      .throttle(1, 1 seconds)
      .log("out")
    // Create the junction elements
    val broadcast = builder.add(Broadcast[DateTime](signAddresses.length))
    val merge = builder.add(Merge[Seq[Byte]](signAddresses.length))

    // Build the graph
    clockSrc ~> broadcast
    clockImageFlows.foreach(
      broadcast ~> _ ~> merge
    )
    merge ~> identity ~> out

    // Indicate the graph is over
    ClosedShape
  }

  def clockSource(interval: FiniteDuration): Source[DateTime, _] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

  private val random = new Random()

  def clockImageFlow(size: (Int, Int)): Flow[DateTime, Image, NotUsed] =
    Flow[DateTime].map(datetime => imageWriter.textToImage(fmt.print(datetime)))

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
