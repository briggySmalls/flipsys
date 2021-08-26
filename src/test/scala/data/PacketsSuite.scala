package data

import data.Packet._

class PacketsSuite extends munit.FunSuite {
  test("no payload packet bytes") {
    testPacketBytes(
      new Packet(1, 2),
      Seq(0x02, '1', '2', 0x03, '9', 'A')
    )
  }

  test("with payload packet bytes") {
    testPacketBytes(
      new Packet(1, 2, payloadOption = Some(Seq(3, 4, 5))),
      Seq(0x02, '1', '2', '0', '3', '0', '4', '0', '5', 0x03, '6', 'E')
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

  test("simple image packet bytes") {
    // Create an image
    val img = new Image(
      Vector.tabulate(4, 2)((row, col) =>
        if (row == 0 && col == 0) true else false
      )
    )
    val packet = new DrawImage(1, img)
    testPacketBytes(
      packet,
      Seq(0x02, '1', '1', '0', '2', '0', '1', '0', '0', 0x03, '7', '8')
    )
  }

  test("tall image packet bytes") {
    // Create an image
    val img = new Image(
      Vector.tabulate(15, 2)((row, col) =>
        if (row == 0 && col == 0) true
        else if (row == 9 && col == 0) true
        else false
      )
    )
    val packet = new DrawImage(1, img)
    testPacketBytes(
      packet,
      Seq(0x02, '1', '1', '0', '4', '0', '1', '0', '2', '0', '0', '0', '0',
        0x03, 'B', '4')
    )
  }

  def testPacketBytes(packet: Packet, bytes: Seq[Byte])(implicit
      loc: munit.Location
  ) = {
    assertEquals(packet.bytes, bytes)
  }
}
