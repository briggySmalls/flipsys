package packets

import flipsys.Image
import flipsys.HanoverByte

object Packet {
  def _imageToInts(image: Image): Seq[Int] = {
    // We pad each column to align with a bytes-worth of data
    val byteAlignedRowCount = _closestLargerMultiple(image.rows, 8)
    val newImage =
      image.image ++ Vector.fill(
        byteAlignedRowCount - image.rows,
        image.columns
      )(false)
    // Interpret each column as a series of whole bytes
    val data = _imageToBits(newImage)
      .grouped(8)
      .map(
        _.zipWithIndex
          .foldLeft(0)((acc, kv) =>
            kv match {
              case (value, index) =>
                if (value) acc + scala.math.pow(2, index).toInt else acc
            }
          )
      )
      .toSeq
    // We prefix with the "resolution" of the data
    // This is essentially a (possibly-truncated) byte count
    (data.length & 0xff) +: data
  }

  def _imageToBits(image: Vector[Vector[Boolean]]): Seq[Boolean] = {
    require(image.length > 0)
    for (
      row <- 0 until image.length;
      col <- 0 until image(0).length
    )
      yield image(row)(col)
  }

  def _closestLargerMultiple(value: Int, base: Int) = {
    (value.floatValue / base).ceil.toInt * base
  }

  object StartTestSigns extends Packet(3, 0)
  object StopTestSigns extends Packet(12, 0)
  class DrawImage(address: Int, image: Image)
      extends Packet(1, address, payloadOption = Some(_imageToInts(image)))
}

class Packet(
    val command: Int,
    val address: Int,
    val payloadOption: Option[Seq[Int]] = None
) {
  val startByte = 0x02.toByte
  val endByte = 0x03.toByte

  def bytes: Seq[Byte] = {
    require(command < scala.math.pow(2, 8))
    require(address < scala.math.pow(2, 8))
    var data: Seq[Byte] =
      Seq(command, address).flatMap(
        HanoverByte(_, isPadded = false).toAsciiHex()
      )
    payloadOption match {
      case None => {}
      case Some(payload) =>
        data ++= payload.flatMap(HanoverByte(_).toAsciiHex())
    }
    val checkSummedData = data :+ endByte
    (startByte +: checkSummedData) ++ HanoverByte(
      _calculateChecksum(checkSummedData)
    ).toAsciiHex()
  }

  def _calculateChecksum(input: Seq[Byte]): Int = {
    val totalClipped = input.sum.toByte & 0xff
    (((totalClipped ^ 0xff) + 1) & 0xff)
  }
}
