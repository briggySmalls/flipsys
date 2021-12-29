package services

import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import config.SignConfig
import services.StreamTypes.DisplayPayload


class DisplayService(sink: Sink[Seq[Byte], _], signs: Seq[SignConfig])(implicit
    materializer: Materializer
) {
  // Create a mergehub so we can keep the sink alive whilst swapping sources
  private val mergedSource = MergeHub.source[Seq[Byte]]
  private val mergedSink = mergedSource.to(sink).run()
  private var killSwitch: Option[KillSwitch] =
    None // State of currently running source

  def start(source: Source[DisplayPayload, _]): Unit = {
    stop()
    killSwitch = Some(
      source
        // Transform payloads into bytes for each sign
        // It's important to keep this in the ephemeral source (rather than the
        // persistent sink on the other side of the merghub) so that any backpressure
        // is killed with the kill switch. Otherwise, we'll start a new source
        // but not see it until all the backpressured payloads are processed
        .via(SignsService.flow(signs))
        // Introduce a killswitch downstream of the signs flow.
        // This is so we "yank" the source without any pending, backpressured
        // images appearing after the switch
        .viaMat(KillSwitches.single)(Keep.right)
        .log("output")
        .to(mergedSink)
        .run()
    )
  }

  def stop(): Unit =
    killSwitch.foreach(_.shutdown)
}
