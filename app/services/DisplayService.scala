package services

import akka.actor.Cancellable
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import services.StreamTypes.DisplayPayload

import scala.util.chaining.scalaUtilChainingOps



class DisplayService(val sources: Map[String, () => Source[DisplayPayload, Cancellable]], val sink: () => Sink[DisplayPayload, _])(implicit materializer: Materializer) {
  // Create a mergehub so we can dynamically swap inputs
  private val mergedSource = MergeHub.source[DisplayPayload]
  private val mergedSink = mergedSource.to(sink()).run()
  private var cancellable: Option[Cancellable] = None // State of currently running source

  def start(source: String): Either[String, Unit] = {
    sources
      .get(source)
      .toRight(s"Source $source not recognised")
      .map(s => {
        // Cancel the current, if running
        stop()
        // Start the new
        s().to(mergedSink).run()
      })
      .tap(c => cancellable = c.toOption)
      .map(c => ())
  }

  def stop(): Unit =
    cancellable.map(_.cancel())
}
