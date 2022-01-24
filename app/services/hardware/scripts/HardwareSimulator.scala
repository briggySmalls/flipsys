package services.hardware.scripts

import akka.actor.ActorSystem
import akka.stream.SinkRef
import akka.stream.scaladsl.{Flow, Sink, StreamRefs}
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

  connect(
    imageSink(ui.imagesSink),
    indicatorSink(ui.indicatorSink)
  )
  ui.run()

  private def connect(
      imagesSink: SinkRef[BytePayload],
      indicatorSink: SinkRef[StatusPayload]
  ) = {
    // Identify the remote simulator actor
    val selection = system.actorSelection(
      "akka://application@127.0.0.1:2551/user/signSinkActor"
    )
    // Inform the remote simulated HAL of the stream entities it can use
    selection ! SimulatorOffer(imagesSink, indicatorSink)
  }

  private def imageSink(imagesSink: Sink[(SignConfig, Image), _]) = {
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

    StreamRefs.sinkRef[BytePayload]().to(sink).run()
  }

  private def indicatorSink(indicatorSink: Sink[Boolean, _]) = {
    val sink = Flow[StatusPayload]
      .map(_.status)
      .to(indicatorSink)
    StreamRefs.sinkRef[StatusPayload]().to(sink).run()
  }
}
