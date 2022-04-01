package services

import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import akka.stream.{KillSwitch, KillSwitches, Materializer}
import config.SignConfig
import models.Frame
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DisplayService(
    sink: Sink[Seq[Byte], _],
    signs: Seq[SignConfig],
    defaultSource: () => Source[Frame, _]
)(implicit
    materializer: Materializer,
    ec: ExecutionContext
) extends Logging {
  // Create a mergehub so we can keep the sink alive whilst swapping sources
  private val mergedSource = MergeHub.source[Frame]
  private val mergedSink = mergedSource
    .mapConcat(f => {
      f.images
    })
    // Transform payloads into bytes for each sign
    .via(SignsService.flow(signs))
    .to(sink)
    .run()
  private var killSwitch: Option[KillSwitch] =
    None // State of currently running source

  def start(source: Source[Frame, _]): Unit = {
    runStream(source, withFallback = true)
  }

  def stop(): Unit =
    killSwitch.foreach(_.shutdown)

  private def runStream(
      source: Source[Frame, _],
      withFallback: Boolean = false
  ): Unit = {
    logger.info(s"starting source: $source")
    stop()
    killSwitch = Some(
      source
        .concat(
          if (withFallback) defaultSource()
          else Source.empty
        ) // Revert back to default when we're done
        .throttle(1, 2 second)
        // Introduce a killswitch downstream of the signs flow.
        // This is so we "yank" the source without any pending, backpressured
        // images appearing after the switch
        .viaMat(KillSwitches.single)(Keep.right)
        .to(mergedSink)
        .run()
    )
  }

  runStream(defaultSource())
}
