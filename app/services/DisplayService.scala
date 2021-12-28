package services

import akka.actor.Cancellable
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import services.StreamTypes.DisplayPayload

import scala.util.chaining.scalaUtilChainingOps



class DisplayService(private val sink: Sink[DisplayPayload, _])(implicit materializer: Materializer) {
  // Create a mergehub so we can dynamically swap inputs
  private val mergedSource = MergeHub.source[DisplayPayload]
  private val mergedSink = mergedSource.to(sink).run()
  private var cancellable: Option[Cancellable] = None // State of currently running source

  def start(source: Source[DisplayPayload, Cancellable]): Unit = {
    stop()
    cancellable = Some(source.to(mergedSink).run())
  }

  def stop(): Unit =
    cancellable.map(_.cancel())
}
