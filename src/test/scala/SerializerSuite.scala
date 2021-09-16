import Serializer.{Write, Written, props}
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import data.Packet.{StartTestSigns, StopTestSigns, DrawImage}
import data.Image

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class SerializerSuite extends munit.FunSuite {
  implicit val system = ActorSystem("SerialSuite")
  val portName = "/dev/tty.usbserial-0001"

  test("send StartTestSigns packet") {
    val us = TestProbe()
    val serial = system.actorOf(props(portName))
    us.send(serial, Write(StopTestSigns.bytes))

    us.expectMsg(1 second, Written)
  }

  test("draw picture") {
    val us = TestProbe()
    val serial = system.actorOf(props(portName))

    val image = new Image(Vector.tabulate(7, 84)((row, col) => {
      def isEven(i: Int) = i % 2 == 0
      if (isEven(row) && isEven(col)) true
      else if (!isEven(row) && !isEven(col)) true
      else false
    }))
    us.send(serial, Write(new DrawImage(1, image).bytes))

    us.expectMsg(1 second, Written)
  }
}
