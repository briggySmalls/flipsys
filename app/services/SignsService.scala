package services

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink}
import clients.SerializerSink
import models.Image
import models.packet.Packet.DrawImage
import services.StreamTypes.DisplayPayload

object SignsService {
  def signsSink(serialPort: String, signs: Map[String, (Int, (Int, Int))]): Sink[DisplayPayload, NotUsed] =
    Sink.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sink = new SerializerSink(serialPort)
      val broadcast = builder.add(Broadcast[(String, Image)](signs.size))
      val signFlows = signs.map({
        case (id, (address, size)) => builder.add(
          Flow[(String, Image)]
            .filter(_._1 == id)
            .map(_._2)
            .log(id, _.toString())
            .via(signFlow(address, size))
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

  def signFlow(address: Int, size: (Int, Int)): Flow[Image, Seq[Byte], NotUsed] =
    Flow[Image].map(image => size match {
        case (width, _) if (width != image.columns) => throw new Error(s"Incompatible image width (${image.columns}, should be ${width})")
        case (_, height) if (height != image.rows) => throw new Error(s"Incompatible image width (${image.rows}, should be ${height})")
        case _ => {
          DrawImage(address, image).bytes
        }
      })
}
