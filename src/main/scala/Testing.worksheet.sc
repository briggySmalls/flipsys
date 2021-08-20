"123".map(_.toChar.toByte)
Seq(12, 0).flatMap(_.toHexString.toUpperCase).mkString.getBytes()

Seq(12, 0)
