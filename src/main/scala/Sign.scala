import akka.NotUsed
import akka.stream.scaladsl.Flow
import data.Image
import data.Packet.DrawImage

object Sign {
  def SignFlow(address: Int, size: (Int, Int)): Flow[Image, Seq[Byte], NotUsed] =
    Flow[Image, Seq[Byte], NotUsed].map(image => size match {
        case (width, _) if (width != image.columns) => throw new Error(s"Incompatible image width ($image.columns, should be ${width})")
        case (_, height) if (height != image.rows) => throw new Error(s"Incompatible image width ($image.rows, should be ${height})")
        case _ => {
          DrawImage(address, image).bytes
        }
      })
}