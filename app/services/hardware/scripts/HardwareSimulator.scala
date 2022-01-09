package services.hardware.scripts

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, StreamRefs}
import com.typesafe.config.ConfigFactory
import models.simulator.{BytePayload, SignSinkOffer}

/** Runnable application for hosting a simulator to connect to the simulated HAL
  */
object HardwareSimulator extends App {
  private val configString = s"""
    |akka.remote.artery.canonical.port = 2552
    |""".stripMargin
  private val config = ConfigFactory
    .parseString(configString)
    .withFallback(ConfigFactory.load())
  private implicit val system: ActorSystem =
    ActorSystem("SimulatorSystem", config)
  // Create a sink to print the images
  val sink = Flow[BytePayload]
    .map(_.bytes)
    .log("simulator", d => d)
    .to(Sink.ignore)
  val ref = StreamRefs.sinkRef[BytePayload]().to(sink).run()
  // Identify the remote simulator actor
  val selection = system.actorSelection(
    "akka://application@127.0.0.1:2551/user/signSinkActor"
  )
  // Inform the remote simulated HAL of the stream entities it can use
  selection ! SignSinkOffer(ref)
}
