package gameEngine.fixed

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._

class FixedPointALU(w: Int = 32, f: Int = 16) extends Module {
  val io = IO(new Bundle {
    val x, y = Input(SInt(w.W))
    val sum, diff, prod = Output(SInt(w.W))
  })
  io.sum := io.x + io.y
  io.diff := io.x - io.y
  io.prod := FixedPointUtils.mul(io.x, io.y, w, f)
}

class FixedPointSpec extends AnyFlatSpec {
  val w = 16
  val f = 8

  /** Converts a Double into its fixed-point raw integer form. It scales the
    * floating-point value by 2^f (where f is the number of fractional bits),
    * rounds to the nearest integer, and wraps it as a BigInt. This raw integer
    * can then be poked directly into an SInt(w.W) to represent the fixed-point
    * value.
    */
  private def toRaw(d: Double): BigInt = BigInt(math.round(d * (1 << f)))

  private def withDut(testBody: FixedPointALU => Unit): Unit =
    simulate(new FixedPointALU(w, f)) { dut => testBody(dut) }

  behavior of "FixedPointALU"

  it should "handle fixed‑point addition" in {
    val cases = Seq(
      ((2.0, 4.0), 6.0),
      ((1.2, 2.8), 4.0),
      ((1.0, 1.5), 2.5),
      ((-1.0, 3.2), 2.2),
      ((0.5, -0.5), 0.0),
      ((-1.05, -1.06), -2.11)
    )

    withDut { dut =>
      for (((x, y), sum) <- cases) {
        dut.io.x.poke(toRaw(x))
        dut.io.y.poke(toRaw(y))
        dut.clock.step()
        dut.io.sum.expect(toRaw(sum))
      }
    }
  }

  it should "handle fixed-point subtraction" in {
    val cases = Seq(
      (5.5, 1.25, 4.25),
      (1.0, 1.5, -0.5),
      (-1.0, -1.5, 0.5),
      (0.5, -0.5, 1.0),
      (-2.2, 3.1, -5.3)
    )

    withDut { dut =>
      for ((x, y, diff) <- cases) {
        dut.io.x.poke(toRaw(x))
        dut.io.y.poke(toRaw(y))
        dut.clock.step()
        dut.io.diff.expect(toRaw(diff))
      }
    }

  }
  it should "handle fixed-point multiplication" in {
    val cases = Seq(
      (2.0, 3.0, 6.0),
      (1.5, 2.0, 3.0),
      (1.5, 1.5, 2.25),
      (-1.0, 3.5, -3.5),
      (-1.5, -2.0, 3.0),
      (0.5, -0.5, -0.25)
    )
    withDut { dut =>
      for ((x, y, prod) <- cases) {
        dut.io.x.poke(toRaw(x))
        dut.io.y.poke(toRaw(y))
        dut.clock.step()
        dut.io.prod.expect(toRaw(prod))
      }
    }
  }

}

class FixedPointUtilsSpec extends AnyFlatSpec {
  behavior of "FixedPointUtils.toFix"

  it should "convert various positive and negative doubles correctly" in {
    val cases = Seq(
      (1.0, 16, 8, BigInt(0x100)), // 1.0 in Q8.8 should be represented as 0x100
      (1.5, 16, 8, BigInt(0x180)),
      (-1.5, 16, 8, BigInt(-0x180)),
      (2.345, 16, 8, BigInt(0x258)), // 0x258 = 2.34375 in Q8.8, slight loss
      (0.0, 16, 8, BigInt(0))
    )
    for ((value, w, f, expected) <- cases) {
      val lit = FixedPointUtils.toFixed(value, w, f)
      assert(
        lit.litValue == expected,
        s"toFix($value, $w, $f).litValue = ${lit.litValue}, expected $expected"
      )
    }
  }

  it should "round to nearest integer when scaling" in {
    // 1.004 * 256 = 257.024, should round to 257
    val lit = FixedPointUtils.toFixed(1.004, 16, 8)
    assert(lit.litValue == BigInt(257))
  }

  it should "handle edge fractional values correctly" in {
    val lit1 = FixedPointUtils.toFixed(0.5, 16, 8) // 0.5*256 = 128
    val lit2 = FixedPointUtils.toFixed(-0.5, 16, 8) // -128
    assert(lit1.litValue == BigInt(128))
    assert(lit2.litValue == BigInt(-128))
  }
}
