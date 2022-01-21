package services.hardware.scripts

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, StreamRefs}
import com.typesafe.config.ConfigFactory
import config.{ApplicationSettings, SignConfig}
import models.Image
import models.packet.Packet
import models.packet.Packet.DrawImage
import models.simulator.{BytePayload, SignSinkOffer}
import play.api.Configuration
import services.hardware.SimulatorUi

/** Runnable application for hosting a simulator to connect to the simulated HAL
  */
object HardwareSimulator extends App {
  private val configString = s"""
    |akka {
    |  stdout-loglevel = "OFF"
    |  loglevel = "OFF"
    |  remote.artery.canonical.port = 2552
    |}
    |""".stripMargin
  private val config = ConfigFactory
    .parseString(configString)
    .withFallback(ConfigFactory.load())
  private implicit val system: ActorSystem =
    ActorSystem("SimulatorSystem", config)

  //  Create the terminal UI
  private val appConfig = new ApplicationSettings(Configuration(config))
  val ui = new SimulatorUi(appConfig.signs)

  connect(ui.imagesSink)
  ui.run()

  private def connect(imagesSink: Sink[(SignConfig, Image), _]) = {
    // Create a sink to print the images
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
      .to(imagesSink)
    val ref = StreamRefs.sinkRef[BytePayload]().to(sink).run()
    // Identify the remote simulator actor
    val selection = system.actorSelection(
      "akka://application@127.0.0.1:2551/user/signSinkActor"
    )
    // Inform the remote simulated HAL of the stream entities it can use
    selection ! SignSinkOffer(ref)
  }
}
