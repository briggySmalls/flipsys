package services.hardware

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import clients.SimulatorReceptionist
import config.hardware.SimulatedHardwareSettings

import javax.inject.{Inject, Singleton}

@Singleton
class SimulatedHardwareImpl @Inject() (
    settings: SimulatedHardwareSettings,
    actorSystem: ActorSystem
) extends HardwareLayer {
  private implicit val mat = actorSystem

  private val (_signsSink, _signsSource) = createBus[Seq[Byte]]()
  private val (_indicatorSink, _indicatorSource) = createBus[Boolean]()
  private val (_pressedSink, _pressedSource) = createBus[Boolean]()

  private val _ =
    actorSystem.actorOf(
      Props(
        new SimulatorReceptionist(_signsSource, _indicatorSource, _pressedSink)
      ),
      "signSinkActor"
    )

  override def serialSink: Sink[Seq[Byte], _] = _signsSink

  override def indicatorSink: Sink[Boolean, _] = _indicatorSink

  override def pressedSource: Source[Boolean, _] = _pressedSource

  // Ensure that the Broadcast output is dropped if there are no listening parties.
  // If this dropping Sink is not attached, then the broadcast hub will not drop any
  // elements itself when there are no subscribers, backpressuring the producer instead.
  _signsSource.runWith(Sink.ignore)
  _indicatorSource.runWith(Sink.ignore)
  _pressedSource.runWith(Sink.ignore)

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
