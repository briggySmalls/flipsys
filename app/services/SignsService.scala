package services

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import config.SignConfig
import models.Image
import models.packet.Packet.DrawImage
import play.api.Logging
import services.StreamTypes.DisplayPayload

import scala.concurrent.duration._
import scala.language.postfixOps

object SignsService extends Logging {
  def flow(
      signs: Seq[SignConfig]
  ): Flow[DisplayPayload, Seq[Byte], NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[(String, Image)](signs.size))
      // Create a flow per sign
      val signFlows = signs.map(c => builder.add(signFlow(c)))
      val merge = builder.add(Merge[Seq[Byte]](signs.size))

      signFlows.foreach(flow => {
        broadcast ~> flow ~> merge
      })

      FlowShape.of(broadcast.in, merge.out)
    })

  private def signFlow(
      config: SignConfig
  ): Flow[DisplayPayload, Seq[Byte], NotUsed] =
    Flow[DisplayPayload]
      .filter(_._1 == config.name) // Listen to the specific sign
//      .throttle(
//        1,
//        2 seconds
//      ) // A single sign can only handle images at a certain rate
      .map { case (_, i) =>
        if (config.flip) i.rotate90().rotate90() else i
      } // Flip if required
      .wireTap(i => logger.info(s"[${config.name}]: writing image"))
      .log(config.name, _.toString())
      .map { image =>
        config.size match {
          case (width, _) if (width != image.columns) =>
            throw new Error(
              s"Incompatible image width (${image.columns}, should be ${width})"
            )
          case (_, height) if (height != image.rows) =>
            throw new Error(
              s"Incompatible image width (${image.rows}, should be ${height})"
            )
          case _ => DrawImage(config.address, image).bytes
        }
      }
}
