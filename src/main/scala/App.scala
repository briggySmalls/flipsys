import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source, Zip}
import com.github.nscala_time.time.Imports.DateTime
import data.Image
import data.Packet.DrawImage

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object App {
  private val size = (84, 7)

  val graph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._ // brings some nice operators in scope

    // Create the elements
    val clockSrc = clockSource(2 seconds)
    val out = Sink.foreach(println)

    val clockImageFlows = List(1, 2).map(
        address => builder.add(
          clockImageFlow(size).via(signFlow(address, size))
        )
    )
//    val serializerSink = builder.add(new SerializerSink("dev/tty.usbserial-0001"))
    // Create the junction elements
    val broadcast = builder.add(Broadcast[DateTime](2))
    val merge = builder.add(Merge[Seq[Byte]](2))

    // Build the graph
    clockSrc ~> broadcast
    broadcast ~> clockImageFlows(0) ~> merge
    broadcast ~> clockImageFlows(1) ~> merge
    merge ~> out

    // Indicate the graph is over
    ClosedShape
  }

  def clockSource(interval: FiniteDuration): Source[DateTime, _] =
    Source.tick(0 second, interval, "tick").map(_ => DateTime.now())

  private val random = new Random()

  def clockImageFlow(size: (Int, Int)): Flow[DateTime, Image, NotUsed] =
    Flow[DateTime].map(_ =>
      new Image(Vector.tabulate(size._2, size._1)((_, _) => random.nextBoolean()))
    )

  def signFlow(address: Int, size: (Int, Int)): Flow[Image, Seq[Byte], NotUsed] =
    Flow[Image].map(image => size match {
        case (width, _) if (width != image.columns) => throw new Error(s"Incompatible image width ($image.columns, should be ${width})")
        case (_, height) if (height != image.rows) => throw new Error(s"Incompatible image width ($image.rows, should be ${height})")
        case _ => {
          DrawImage(address, image).bytes
        }
      })
}
