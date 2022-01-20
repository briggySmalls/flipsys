package scala.models.packet

import models.packet.HanoverByte
import org.scalacheck.Gen
import org.scalacheck.Prop.forAllNoShrink
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.Checkers.check
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class HanoverByteSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  "HanoverByte" should "roundtrip" in {
    check {
      forAllNoShrink(Gen.choose(0, 255)) { i =>
        val b = HanoverByte(i)
        b === HanoverByte.fromAsciiHex(b.toAsciiHex())
      }
    }
  }
}
