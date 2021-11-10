package clients

import akka.stream.{Attributes, Inlet, SinkShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, StageLogging}
import com.fazecast.jSerialComm.SerialPort

// Custom akka sink for serial comms
class SerializerSink(port: String) extends GraphStage[SinkShape[Seq[Byte]]] {
  val comPort = SerialPort.getCommPort(port)

  val in: Inlet[Seq[Byte]] = Inlet("clients.SerializerSink")
  override val shape: SinkShape[Seq[Byte]] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {

      override def preStart(): Unit = {
        // Open the port
        log.debug("Opening port...")
        comPort.setComPortParameters(4800, 8, 1, 0)
        comPort.openPort()
        log.debug("Port opened!")
        // Request first element
        pull(in)
      }

      override def postStop(): Unit = {
        comPort.closePort()
        log.debug("Port closed!")
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
