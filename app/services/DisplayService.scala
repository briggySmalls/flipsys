package services

import akka.NotUsed
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, MergeHub, Sink, Source}
import akka.stream.{FlowShape, KillSwitch, KillSwitches, Materializer}
import config.SignConfig
import models.Frame
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DisplayService(
    signs: Seq[SignConfig],
    defaultSource: () => Source[Frame, _]
)(implicit
    materializer: Materializer,
    ec: ExecutionContext
) extends Logging {
  // Create a mergehub so we can keep the sink alive whilst swapping sources
  private val (mergedSink, mergedSource) =
    MergeHub.source[Frame].preMaterialize()

  val flow = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val streamCreator = builder.add(runSources)
      val expand = builder.add(Flow[Frame].mapConcat(f => f.images))
      val signsFlow = builder.add(SignsService.flow(signs))

      // Process all downstream frames through signs
      mergedSource ~> expand ~> signsFlow

      FlowShape(
        streamCreator.in,
        signsFlow.out
      )
  })

  private def runSources: Sink[Source[Frame, _], _] =
    Flow[Source[Frame, _]]
      .scan[Option[KillSwitch]](None) { (maybeKs, source) =>
        {
          logger.info(s"Stopping previous display stream: ${maybeKs}")
          maybeKs.foreach(_.shutdown())
          logger.info(s"starting source: $source")
          Some(
            source
              .concat(
                defaultSource()
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
      }
      .to(Sink.ignore)
}
