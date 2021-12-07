package services

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink}
import clients.SerializerSink
import config.SignConfig
import models.Image
import models.packet.Packet.DrawImage
import services.StreamTypes.DisplayPayload

object SignsService {
  def signsSink(serialPort: String, signs: Seq[SignConfig]): Sink[DisplayPayload, NotUsed] =
    Sink.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sink = new SerializerSink(serialPort)
      val broadcast = builder.add(Broadcast[(String, Image)](signs.size))
      val signFlows = signs.map(c => {
        builder.add(
          Flow[(String, Image)]
            .filter(_._1 == c.name)
            .map(_._2)
            .log(c.name, _.toString())
            .via(signFlow(c))
        )
      })
      val merge = builder.add(Merge[Seq[Byte]](signs.size))
      val flip = builder.add(Flow[(String, Image)].map({case (id, image) =>
        if (id == "top")
          (id, image.rotate90().rotate90())
        else
          (id, image)
      }))

      flip ~> broadcast
      signFlows.foreach( flow => {
          broadcast ~> flow ~> merge
      })
      merge ~> sink

      SinkShape.of(flip.in)
    })

  def signFlow(config: SignConfig): Flow[Image, Seq[Byte], NotUsed] =
    Flow[Image].map(image => config.size match {
        case (width, _) if (width != image.columns) => throw new Error(s"Incompatible image width (${image.columns}, should be ${width})")
        case (_, height) if (height != image.rows) => throw new Error(s"Incompatible image width (${image.rows}, should be ${height})")
        case _ => {
          DrawImage(config.address, image).bytes
        }
      })
}
