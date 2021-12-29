package services

import akka.actor.Cancellable
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import services.StreamTypes.DisplayPayload

import scala.util.chaining.scalaUtilChainingOps

class DisplayService(
    val sink: () => Sink[DisplayPayload, _]
)(implicit materializer: Materializer) {
  var killSwitch: Option[KillSwitch] = None

  def start(source: Source[DisplayPayload, _]): Unit = {
    killSwitch.foreach(_.shutdown())
    killSwitch = Some(
      source
        .viaMat(KillSwitches.single)(Keep.right)
        .to(sink())
        .run()
    )
  }

  def stop(): Unit =
    killSwitch.foreach(_.shutdown())
}
