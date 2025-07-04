package gameEngine.fixed

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.FixedPointUtils._

class FixedPointALU extends Module {
  val io = IO(new Bundle {
    val x, y = Input(SInt(width.W))
    val sum, diff, prod, floor, ceil, fracPart, round = Output(SInt(width.W))
  })

  io.sum := io.x + io.y
  io.diff := io.x - io.y
  io.prod := io.x.fpMul(io.y)

  io.floor := io.x.fpFloor
  io.ceil := io.x.fpCeil
  io.fracPart := io.x.fpFrac
  io.round := io.x.fpRound
}

class FixedPointALUSpec extends AnyFlatSpec {
  behavior of "FixedPointALU"

  private val w = width // 32
  private val f = frac // 16

  // Helper to convert a Double to raw fixed-point bits
  private def toRaw(d: Double): BigInt = BigInt(math.round(d * (1 << f)))

  private def withDut(testBody: FixedPointALU => Unit): Unit =
    simulate(new FixedPointALU) { dut => testBody(dut) }

  it should "handle fixed-point addition" in {
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
      (0.5, -0.5, -0.25),
      (0.5, 0.5, 0.25),
      (-1.5, 1.0, -1.5),
      (1.25, 2.00, 2.50),
      (1.0, 0.5, 0.5),

      // positive overflow
      (maxDouble, 2.0, maxDouble),

      // negative overflow
      (minDouble, 2.0, minDouble)
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

  it should "compute floor, ceil and frac correctly" in {
    val cases = Seq(
      (2.75, 2.0),
      (2.00, 2.0), // exact integer
      (-2.25, -3.0),
      (-2.00, -2.0),
      (0.99, 0.0),
      (-0.01, -1.0),
      (-2.7, -3.0)
    )
    withDut { dut =>
      for ((in, expected) <- cases) {
        dut.io.x.poke(toRaw(in))
        dut.clock.step()
        dut.io.floor.expect(toRaw(expected))
      }
    }
  }

  it should "compute ceil correctly" in {
    val cases = Seq(
      (2.75, 3.0),
      (2.00, 2.0), // no bump for exact integer
      (-2.25, -2.0),
      (-2.00, -2.0),
      (0.01, 1.0),
      (-0.99, 0.0),
      (-2.7, -2.0)
    )
    withDut { dut =>
      for ((in, expected) <- cases) {
        dut.io.x.poke(toRaw(in))
        dut.clock.step()
        dut.io.ceil.expect(toRaw(expected))
      }
    }
  }

  it should "extract fractional part correctly" in {
    val cases = Seq(
      (2.75, 0.75),
      (2.00, 0.00),
      (-1.25, 0.25),
      (-2.00, 0.00),
      (0.50, 0.50),
      (-0.50, 0.50) // fracPart is magnitude only
    )
    withDut { dut =>
      for ((in, expectedFrac) <- cases) {
        dut.io.x.poke(toRaw(in))
        dut.clock.step()
        dut.io.fracPart.expect(toRaw(expectedFrac))
      }
    }
  }

  it should "round correctly" in {
    val cases = Seq(
      (2.75, 3.0),
      (2.00, 2.0),
      (-1.25, -1.0),
      (-2.00, -2.0),
      (0.50, 1.0),
      (-0.50, -1.0)
    )
    withDut { dut =>
      for ((in, expected) <- cases) {
        dut.io.x.poke(toRaw(in))
        dut.clock.step()
        val got = dut.io.round.peek()
        withClue(
          f"Rounding ${in}. Expected ${expected}, got ${got.toDouble}"
        ) {
          assert(got.toDouble == expected)
        }
      }
    }
  }

}

class FixedPointUtilsSpec extends AnyFlatSpec {
  behavior of "FixedPointUtils"

  it should "convert various doubles correctly" in {
    val cases = Seq(
      (1.0, BigInt(1 << frac)),
      (1.5, BigInt((1.5 * (1 << frac)).round)),
      (-1.5, BigInt((-1.5 * (1 << frac)).round)),
      (2.345, BigInt((2.345 * (1 << frac)).round)),
      (0.0, BigInt(0))
    )
    for ((value, expected) <- cases) {
      val lit = toFP(value)
      assert(
        lit.litValue == expected,
        s"toFP($value).litValue = ${lit.litValue}, expected $expected"
      )
    }
  }

  it should "round to nearest integer when scaling" in {
    // 1.004 * 65536 = 65701.344 -> 65701
    val lit = toFP(1.004)
    assert(lit.litValue == BigInt(math.round(1.004 * (1 << frac))))
  }

}
