package services.hardware.scripts

import akka.actor.ActorSystem
import akka.stream.SinkRef
import akka.stream.scaladsl.{Flow, Sink, Source, StreamRefs}
import clients.SimulatorReceptionist.SimulatorOffer
import com.typesafe.config.ConfigFactory
import config.{ApplicationSettings, SignConfig}
import models.Image
import models.packet.Packet
import models.packet.Packet.DrawImage
import models.simulator.{BytePayload, StatusPayload}
import play.api.Configuration
import services.hardware.SimulatorUi

/** Runnable application for hosting a simulator to connect to the simulated HAL
  */
object HardwareSimulator extends App {
  // Create an actor system
  private implicit val system: ActorSystem =
    ActorSystem("SimulatorSystem", config)

  //  Create the terminal UI
  private val appConfig = new ApplicationSettings(Configuration(config))
  val ui = new SimulatorUi(appConfig.signs)

  // Connect to the remote simulated hardware layer
  connect()

  // Run the UI
  ui.run()

  private def connect() = {
    // Identify the remote simulator actor
    val selection = system.actorSelection(
      "akka://application@127.0.0.1:2551/user/signSinkActor"
    )
    // Inform the remote simulated HAL of the stream entities it can use
    selection ! SimulatorOffer(imageSink, indicatorSink, buttonSource)
  }

  private def imageSink = {
    val sink = Flow[BytePayload]
      .map(_.bytes)
      .map(bs => {
        (Packet.addressFromBytes(bs), bs)
      })
      .collect { case (Some(address), bs) =>
        (appConfig.signs.find(_.address == address), bs)
      } // Drop packets that fail address extraction
      .collect { case (Some(sign), bs) =>
        (sign, DrawImage.fromBytes(bs, sign.size._2))
      } // Drop packets that don't match a current sign
      .collect { case (sign, Some(image)) =>
        (sign, image.image)
      } // Drop packets fail packet extraction
      .to(ui.imagesSink)

    StreamRefs.sinkRef[BytePayload]().to(sink).run()
  }

  private def indicatorSink = {
    val sink = Flow[StatusPayload]
      .map(_.status)
      .to(ui.indicatorSink)
    StreamRefs.sinkRef[StatusPayload]().to(sink).run()
  }

  private def buttonSource = {
    val sourceRef = StreamRefs.sourceRef[StatusPayload]()

    ui.buttonSource
      .map(StatusPayload)
      .runWith(sourceRef)
  }

  private def config =
    ConfigFactory
      .parseString("""
         |akka {
         |  stdout-loglevel = "OFF"
         |  loglevel = "OFF"
         |  remote.artery.canonical.port = 2552
         |}
         |""".stripMargin)
      .withFallback(ConfigFactory.load())
}
