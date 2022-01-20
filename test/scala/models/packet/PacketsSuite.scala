package models.packet

import models.Image
import models.packet.Packet._
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec

class PacketsSuite
    extends AnyWordSpec
    with Matchers
    with TableDrivenPropertyChecks {

  val imageTable = Table(
    ("example", "packet", "bytes"),
    (
      "simple image packet", {
        val img = Image(
          Vector.tabulate(4, 2)((row, col) =>
            if (row == 0 && col == 0) true else false
          )
        )
        new DrawImage(1, img)
      },
      Seq(0x02, '1', '1', '0', '2', '0', '1', '0', '0', 0x03, '7', '8')
    ),
    (
      "tall image packet", {
        val img = Image(
          Vector.tabulate(15, 2)((row, col) =>
            if (row == 0 && col == 0) true
            else if (row == 9 && col == 0) true
            else false
          )
        )
        new DrawImage(1, img)
      },
      Seq(0x02, '1', '1', '0', '4', '0', '1', '0', '2', '0', '0', '0', '0',
        0x03, 'B', '4')
    )
  )

  "A Packet" should {
    "have bytes field" which
      forAll(
        Table(
          ("example", "packet", "bytes"),
          (
            "no payload packet",
            new Packet(command = 1, address = 2) {},
            Seq(0x02, '1', '2', 0x03, '9', 'A')
          ),
          (
            "small payload packet",
            new Packet(1, 2) { override def payload = Seq(3, 4, 5) },
            Seq(0x02, '1', '2', '0', '3', '0', '4', '0', '5', 0x03, '6', 'E')
          ),
          (
            "start test signs packet",
            StartTestSigns,
            Seq(0x02, '3', '0', 0x03, '9', 'A')
          ),
          (
            "stop test signs packet",
            StopTestSigns,
            Seq(0x02, 'C', '0', 0x03, '8', 'A')
          )
        ) ++ imageTable
      ) { (example, packet, bytes) =>
        s"correctly represents $example" in {
          packet.bytes shouldEqual bytes
        }
      }

    "convert bytes to DrawImage" which
      forAll(imageTable) { (example, image, bytes) =>
        s"handles $example" in {
          DrawImage.fromBytes(
            bytes.map(_.toByte),
            image.image.rows
          ) should contain(image)
        }
      }
  }
}
