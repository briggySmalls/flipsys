package packets
import Packet._

class PacketsSuite extends munit.FunSuite {
  test("no payload packet bytes") {
    testPacketBytes(
      new Packet(1, 2),
      Seq(0x02, '1', '2', 0x03, '9', 'A')
    )
  }

  test("with payload packet bytes") {
    testPacketBytes(
      new Packet(1, 2, payloadOption = Some(Seq('3', '4', '5'))),
      Seq(0x02, '1', '2', '3', '4', '5', 0x03, 'F', 'E')
    )
  }

  test("StartTestSigns packet bytes") {
    testPacketBytes(
      StartTestSigns,
      Seq(0x02, '3', '0', 0x03, '9', 'A')
    )
  }

  test("StopTestSigns packet bytes") {
    testPacketBytes(
      StopTestSigns,
      Seq(0x02, 'C', '0', 0x03, '8', 'A')
    )
  }

  def testPacketBytes(packet: Packet, bytes: Seq[Byte])(implicit
      loc: munit.Location
  ) = {
    assertEquals(packet.bytes, bytes)
  }
}
