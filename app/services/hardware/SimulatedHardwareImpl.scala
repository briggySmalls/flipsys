package services.hardware

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import clients.SignSinkActor
import config.hardware.SimulatedHardwareSettings

import javax.inject.{Inject, Singleton}

@Singleton
class SimulatedHardwareImpl @Inject() (
    settings: SimulatedHardwareSettings,
    actorSystem: ActorSystem
) extends HardwareLayer {
  private implicit val mat = actorSystem

  private val (signsSink, signsSource) = createBus[Seq[Byte]]
  private val _ =
    actorSystem.actorOf(Props(new SignSinkActor(signsSource)), "signSinkActor")

  def serialSink = signsSink

  // Ensure that the Broadcast output is dropped if there are no listening parties.
  // If this dropping Sink is not attached, then the broadcast hub will not drop any
  // elements itself when there are no subscribers, backpressuring the producer instead.
  signsSource.runWith(Sink.ignore)

  /** Obtain a Sink and Source which will publish and receive from the "bus"
    * respectively.
    * @tparam T
    *   Stream contents
    * @return
    *   A sink and source for the bus
    */
  private def createBus[T](): (Sink[T, NotUsed], Source[T, NotUsed]) =
    MergeHub
      .source[T]
      .toMat(BroadcastHub.sink)(Keep.both)
      .run()
}
