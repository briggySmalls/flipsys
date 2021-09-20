import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import com.fazecast.jSerialComm.{SerialPort, SerialPortDataListener, SerialPortEvent}

object Serializer {
  case class Write(data: Seq[Byte])
  object Written

  def props(port: String) = Props(new Serializer(port))
}

class Serializer(port: String) extends Actor with ActorLogging {
  import Serializer._

  val comPort = SerialPort.getCommPort(port)

  override def preStart() = {
    comPort.setComPortParameters(4800, 8, 1, 0)
    comPort.openPort()
  }

  def receive = LoggingReceive {
    case Write(data) => comPort.writeBytes(data.toArray, data.length)
  }

  override def postStop() = {
    comPort.closePort()
  }
}
