import akka.stream.Attributes
import akka.stream.Inlet
import akka.stream.SinkShape
import akka.stream.stage.GraphStage
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import com.fazecast.jSerialComm.SerialPort

// Custom akka sink for serial comms
class SerializerSink(port: String) extends GraphStage[SinkShape[Seq[Byte]]] {
  val comPort = SerialPort.getCommPort(port)

  val in: Inlet[Seq[Byte]] = Inlet("SerializerSink")
  override val shape: SinkShape[Seq[Byte]] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      override def preStart(): Unit = {
        // Open the port
        comPort.setComPortParameters(4800, 8, 1, 0)
        comPort.openPort()
        // Request first element
        pull(in)
      }

      override def postStop(): Unit = {
        comPort.closePort()
      }

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          write(grab(in))
          pull(in)
        }
      })
    }

  private def write(data: Seq[Byte]) = comPort.writeBytes(data.toArray, data.length)
}
