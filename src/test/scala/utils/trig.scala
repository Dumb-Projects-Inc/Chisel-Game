package gameEngine.trig

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.FixedPointUtils._

class TrigLUTSpec extends AnyFlatSpec {
  behavior of "TrigLUT"

  private val fullTurn = (0 until 16).map(i => i * math.Pi / 8.0)
  private val extras = Seq(
    -math.Pi / 4, // ‑45 °
    -3 * math.Pi / 8, // ‑67.5 °
    2 * math.Pi + math.Pi / 4, // 450 °
    -2 * math.Pi + math.Pi / 4, // -450 °
    4 * math.Pi, // 2 full turns
    math.Pi / 2 + 1e-4, // tiny offset from π/2
    math.Pi - 1e-4 // tiny offset from   π
  )

  private val testAngles = (fullTurn ++ extras).distinct

  // avoid divided by zero errors from secant tests
  private val testAngelsSec =
    testAngles.filter(a => math.abs(math.cos(a)) > 1e-4)

  it should "correctly compute sin, cos, and secant for sample-aligned angles" in {
    // Fixed point arithemetic isn't super accurate.
    val maxError = 0.04
    simulate(new TrigLUT) { dut =>
      for (angle <- testAngles) {

        val rawAngle = toFP(angle)
        dut.io.angle.poke(rawAngle)
        dut.clock.step()

        val expSin = math.sin(angle)
        val expCos = math.cos(angle)

        val rawSin = toFP(expSin)
        val rawCos = toFP(expCos)

        val gotSin = dut.io.sin.peek()
        val gotCos = dut.io.cos.peek()

        assert(
          (gotSin.toDouble - rawSin.toDouble).abs < maxError,
          s"sin mismatch for angle $angle: got ${gotSin.toDouble}(${gotSin.litValue}), expected ${rawSin.toDouble}(${rawSin.litValue})"
        )

        assert(
          (gotCos.toDouble - rawCos.toDouble).abs < maxError,
          s"cos mismatch for angle $angle: got ${gotCos.toDouble}(${gotCos.litValue}), expected ${rawCos.toDouble}(${rawCos.litValue})"
        )

      }

      for (angle <- testAngelsSec) {

        val rawAngle = toFP(angle)
        dut.io.angle.poke(rawAngle)
        dut.clock.step()

        Console.err.println(math.cos(angle))
        val expSec = 1.0 / math.cos(angle)
        Console.err.println(expSec)
        val rawSec = toFP(expSec)
        val gotSec = dut.io.sec.peek()
        assert(
          (gotSec.toDouble - rawSec.toDouble).abs < maxError,
          s"sec mismatch for angle $angle: got ${gotSec.toDouble}(${gotSec.litValue}), expected ${rawSec.toDouble}(${rawSec.litValue})"
        )
      }
    }
  }
}
