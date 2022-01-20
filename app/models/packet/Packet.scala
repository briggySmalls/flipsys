package models.packet

import models.Image

abstract class Packet(command: Int, address: Int) {
  protected def payload: Seq[Int] = Seq.empty

  def bytes: Seq[Byte] = {
    require(command < scala.math.pow(2, 8))
    require(address < scala.math.pow(2, 8))
    // Start with a single byte for the command and address respectively
    val headerBytes = {
      Seq(command, address).map(
        HanoverByte(_, isPadded = false)
      )
    }
    val payloadBytes = payload.map(HanoverByte(_))
    val checkSummedData =
      (headerBytes ++ payloadBytes).flatMap(_.toAsciiHex()) :+ Packet.endByte
    // Assemble together
    (Packet.startByte +: checkSummedData) ++ HanoverByte(
      _calculateChecksum(checkSummedData)
    ).toAsciiHex()
  }

  def _calculateChecksum(input: Seq[Byte]): Int = {
    val totalClipped = input.sum.toByte & 0xff
    (((totalClipped ^ 0xff) + 1) & 0xff)
  }
}

object Packet {
  val startByte: Byte = 0x02.toByte
  val endByte: Byte = 0x03.toByte

  object StartTestSigns extends Packet(command = 3, address = 0)
  object StopTestSigns extends Packet(command = 12, address = 0)

  case class DrawImage(address: Int, image: Image)
      extends Packet(command = 1, address = address) {

    protected override def payload: Seq[Int] = {
      // We pad each column to align with a bytes-worth of data
      val byteAlignedRowCount = closestLargerMultiple(image.rows, 8)
      val newImage = Image(
        image.data ++ Vector.fill(
          byteAlignedRowCount - image.rows,
          image.columns
        )(false)
      )
      // Interpret each column as a series of whole bytes
      val data = imageToBits(newImage)
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

    private def imageToBits(image: Image): Seq[Boolean] = {
      for (
        col <- 0 until image.columns;
        row <- 0 until image.rows
      )
        yield image.data(row)(col)
    }

    private def closestLargerMultiple(value: Int, base: Int) = {
      (value.floatValue / base).ceil.toInt * base
    }
  }
}
