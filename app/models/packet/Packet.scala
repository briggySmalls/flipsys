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

  val startTestCommand = 3
  val stopTestCommand = 12
  val drawImageCommand = 1

  def addressFromBytes(bytes: Seq[Byte]): Option[Int] = bytes match {
    case Packet.startByte +: _ +: address +: _ =>
      Some(HanoverByte.fromAsciiHex(Seq(address)).value)
    case _ =>
      None
  }

  object StartTestSigns extends Packet(command = startTestCommand, address = 0)
  object StopTestSigns extends Packet(command = stopTestCommand, address = 0)

  case class DrawImage(address: Int, image: Image)
      extends Packet(command = drawImageCommand, address = address) {

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
  }

  object DrawImage {
    def fromBytes(bytes: Seq[Byte], height: Int): Option[DrawImage] =
      bytes match {
        case Packet.startByte +: commandByte +: addressByte +: _ +: _ +: tail
            if (HanoverByte
              .fromAsciiHex(Seq(commandByte))
              .value == Packet.drawImageCommand) => {
          val address = HanoverByte.fromAsciiHex(Seq(addressByte)).value
          val rawPayload =
            tail.takeWhile(_ != Packet.endByte) // Drop checksum etc
          val intPayload =
            rawPayload
              .grouped(2)
              .map(HanoverByte.fromAsciiHex(_).value)
              .toSeq // Convert to ints
          val padding = closestLargerMultiple(height, 8)
          val stringCols = intPayload
            .grouped(padding / 8) // Group into columns
            .map(
              _.flatMap(i =>
                // Convert integer to a zero-padded, big-endian binary string
                "%8s".format(i.toBinaryString).replace(" ", "0").reverse
              )
                .dropRight(padding - height) // Drop the byte-aligned padding
                .mkString
            )
            .toVector
          val bits = stringCols
            .map(_.map {
              case '0' => false
              case '1' => true
            }.toVector)
          Some(DrawImage(address, Image(bits).transpose()))
        }
        case _ => None
      }
  }

  def closestLargerMultiple(value: Int, base: Int): Int = {
    (value.floatValue / base).ceil.toInt * base
  }
}
