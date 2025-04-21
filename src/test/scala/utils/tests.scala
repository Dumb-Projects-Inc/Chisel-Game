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
  io.prod := FixedPointUtils.fixMul(io.x, io.y, w, f)
}

class FixedPointSpec extends AnyFlatSpec {
  val w = 16
  val f = 8

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
        dut.io.x := FixedPointUtils.toFix(x, w, f)
        dut.io.y := FixedPointUtils.toFix(y, w, f)
        dut.clock.step()
        dut.io.sum.expect(toRaw(sum))
      }
    }
  }

  it should "handle fixed‑point subtraction" in {
    val cases = Seq(
      ((5.0, 3.5), 1.5),
      ((2.2, 4.1), -1.9),
      ((-1.5, -0.5), -1.0),
      ((0.0, 1.0), -1.0)
    )
    withDut { dut =>
      for (((x, y), diff) <- cases) {
        dut.io.x := FixedPointUtils.toFix(x, w, f)
        dut.io.y := FixedPointUtils.toFix(y, w, f)
        dut.clock.step()
        dut.io.diff.expect(toRaw(diff))
      }
    }
  }

  it should "handle fixed‑point multiplication" in {
    val cases = Seq(
      ((2.0, 4.0), 8.0),
      ((1.5, 2.0), 3.0),
      ((-1.0, 3.0), -3.0),
      ((-1.5, -2.0), 3.0),
      ((0.5, 0.5), 0.25)
    )
    withDut { dut =>
      for (((x, y), prod) <- cases) {
        dut.io.x := FixedPointUtils.toFix(x, w, f)
        dut.io.y := FixedPointUtils.toFix(y, w, f)
        dut.clock.step()
        dut.io.prod.expect(toRaw(prod))
      }
    }
  }
}
