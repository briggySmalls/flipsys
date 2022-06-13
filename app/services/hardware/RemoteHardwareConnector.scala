package services.hardware

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}

trait RemoteHardwareConnector {
  implicit val mat: ActorSystem

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
