package packets

import java.nio.charset.StandardCharsets

object Packet {
  object StartTestSigns extends Packet(3, 0)
  object StopTestSigns extends Packet(12, 0)
}

class Packet(
    val command: Int,
    val address: Int,
    val payloadOption: Option[Seq[Byte]] = None
) {
  val startByte = 0x02.toByte
  val endByte = 0x03.toByte

  def bytes: Seq[Byte] = {
    var data: Seq[Byte] = _toAsciiHex(Seq(command, address))
    payloadOption match {
      case None          => {}
      case Some(payload) => data ++= payload
    }
    val checkSummedData = data :+ endByte
    (startByte +: checkSummedData) ++ _toAsciiHex(
      Seq(_calculateChecksum(checkSummedData))
    )
  }

  def _calculateChecksum(input: Seq[Byte]): Int = {
    val totalClipped = input.sum & 0xff
    (((totalClipped ^ 0xff) + 1) & 0xff)
  }

  def _toAsciiHex(values: Seq[Int]): Seq[Byte] = {
    values
      .flatMap(_.toHexString.toUpperCase)
      .mkString
      .getBytes(StandardCharsets.US_ASCII)
      .toIndexedSeq
  }
}
