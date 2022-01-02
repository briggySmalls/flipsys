package services

import akka.stream.{KillSwitch, KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import config.SignConfig
import play.api.Logging
import services.StreamTypes.DisplayPayload

import scala.concurrent.ExecutionContext
import scala.util.Failure

class DisplayService(
    sink: Sink[Seq[Byte], _],
    signs: Seq[SignConfig],
    defaultSource: () => Source[DisplayPayload, _]
)(implicit
    materializer: Materializer,
    ec: ExecutionContext
) extends Logging {
  // Create a mergehub so we can keep the sink alive whilst swapping sources
  private val mergedSource = MergeHub.source[Seq[Byte]]
  private val mergedSink = mergedSource.to(sink).run()
  private var killSwitch: Option[KillSwitch] =
    None // State of currently running source

  def start(source: Source[DisplayPayload, _]): Unit = {
    runStream(source, withFallback = true)
  }

  def stop(): Unit =
    killSwitch.foreach(_.shutdown)

  private def runStream(
      source: Source[DisplayPayload, _],
      withFallback: Boolean = false
  ): Unit = {
    logger.info(s"starting source: $source")
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
        .watchTermination() { (killSwitch, future) =>
          if (withFallback) future.onComplete {
            case Failure(exception) =>
              logger.error("Stream failed:", exception)
              runStream(defaultSource())
            case _ =>
              logger.debug("Stream completed, starting default")
              runStream(defaultSource())
          }
          killSwitch
        }
        .to(mergedSink)
        .run()
    )
  }

  runStream(defaultSource())
}
