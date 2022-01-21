package models.simulator

/** Serializable message for bytes sent to simulator signs
  * @param bytes
  *   The bytes sent
  */
case class BytePayload(bytes: Array[Byte]) extends FlipsysSerializable
