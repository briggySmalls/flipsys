package models

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
