package services

import akka.actor.Cancellable
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import services.StreamTypes.DisplayPayload

import scala.util.chaining.scalaUtilChainingOps



class DisplayService(val sources: Map[String, () => Source[DisplayPayload, _]], val sink: () => Sink[DisplayPayload, _])(implicit materializer: Materializer) {
  var killSwitch: Option[KillSwitch] = None

  def start(source: String): Either[String, Unit] = {
    killSwitch.foreach(_.shutdown())
    sources
      .get(source)
      .toRight(s"Source $source not recognised")
      .map(s => cancellableStream(s()))
      .tap(e => killSwitch = e.toOption)
      .map(_ => ())
  }

  private def cancellableStream(source: Source[DisplayPayload, _]): KillSwitch =
    source
    .viaMat(KillSwitches.single)(Keep.right)
    .to(sink())
    .run()
}
