package models.packet

import java.nio.charset.StandardCharsets

case class HanoverByte(val value: Int, val isPadded: Boolean = true) {
  def toAsciiHex() = {
    var str = value.toHexString.toUpperCase
    if (isPadded && str.length == 1)
      str = '0' +: str // Pad the string before continuing
    str
      .getBytes(StandardCharsets.US_ASCII)
      .toIndexedSeq
  }
}

object HanoverByte {
  def fromAsciiHex(bytes: Seq[Byte]): HanoverByte = {
    val str = new String(bytes.toArray, StandardCharsets.US_ASCII)
    HanoverByte(Integer.parseInt(str, 16))
  }
}
