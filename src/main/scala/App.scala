import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Zip}
import com.github.nscala_time.time.Imports.DateTime

import scala.concurrent.duration._
import scala.language.postfixOps

object App {
  private val size = (84, 7)

  val graph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._ // brings some nice operators in scope

    // Create the elements
    val clockSource = builder.add(Clock.clockSource(2 seconds))
    val clockImageFactory = new Clock()
    val clockImageFlows = List(1, 2).map(
        address => builder.add(
          clockImageFactory.clockImageFlow(size).via(Sign.SignFlow(address, size))
        )
    )
    val serializerSink = builder.add(new SerializerSink("dev/tty.usbserial-0001"))
    // Create the junction elements
    val broadcast = builder.add(Broadcast[DateTime](2))
    val zip = builder.add(Zip[Seq[Byte], Seq[Byte]]())

    // Build the graph
    clockSource ~> broadcast
    broadcast.out(0) ~> clockImageFlows(1) ~> zip.in0
    broadcast.out(1) ~> clockImageFlows(2) ~> zip.in1
    zip.out ~> serializerSink

    ClosedShape
  }
}
