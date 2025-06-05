package gameEngine.trig

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.FixedPointUtils._

class TrigLUTSpec extends AnyFlatSpec {
  behavior of "TrigLUT"

  private val fullTurn = (0 until 16).map(i => i * math.Pi / 8.0)
  private val extras = Seq(
    -math.Pi / 4, // -45°
    -3 * math.Pi / 8, // -67.5°
    2 * math.Pi + math.Pi / 4, // 450°
    -2 * math.Pi + math.Pi / 4, // -450°
    4 * math.Pi, // 2 full turns
    math.Pi / 2 + 0.05, // tiny offset from π/2
    math.Pi - 0.05 // tiny offset from π
  )

  private val testAngles = (fullTurn ++ extras).distinct
  private val testAnglesNoCosZero =
    testAngles.filter(a => math.abs(math.cos(a)) > 1e-2)
  private val testAnglesNoSinZero =
    testAngles.filter(a => math.abs(math.sin(a)) > 1e-2)

  it should "correctly compute sin and cos for sample-aligned angles" in {
    val maxError = 0.02
    simulate(new TrigLUT) { dut =>
      for (angle <- testAngles) {
        val rawAngle = toFP(angle)
        dut.io.angle.poke(rawAngle)
        dut.clock.step()

        // sine
        val expSin = toFP(math.sin(angle))
        val gotSin = dut.io.sin.peek()
        assert(
          (gotSin.toDouble - expSin.toDouble).abs < maxError,
          s"sin mismatch for angle $angle: got ${gotSin.toDouble}(${gotSin.litValue}), expected ${expSin.toDouble}(${expSin.litValue})"
        )

        // cosine
        val expCos = toFP(math.cos(angle))
        val gotCos = dut.io.cos.peek()
        assert(
          (gotCos.toDouble - expCos.toDouble).abs < maxError,
          s"cos mismatch for angle $angle: got ${gotCos.toDouble}(${gotCos.litValue}), expected ${expCos.toDouble}(${expCos.litValue})"
        )
      }
    }
  }

  it should "correctly compute sec and tan for angles with non-zero cosine" in {
    val maxError = 2
    simulate(new TrigLUT) { dut =>
      for (angle <- testAnglesNoCosZero) {
        val rawAngle = toFP(angle)
        dut.io.angle.poke(rawAngle)
        dut.clock.step()

        // secant
        val expSec = toFP(1.0 / math.cos(angle))
        val gotSec = dut.io.sec.peek()
        assert(
          (gotSec.toDouble - expSec.toDouble).abs < maxError,
          s"sec mismatch for angle $angle: got ${gotSec.toDouble}(${gotSec.litValue}), expected ${expSec.toDouble}(${expSec.litValue})"
        )

        // tangent
        val expTan = toFP(math.tan(angle))
        val gotTan = dut.io.tan.peek()
        assert(
          (gotTan.toDouble - expTan.toDouble).abs < maxError,
          s"tan mismatch for angle $angle: got ${gotTan.toDouble}(${gotTan.litValue}), expected ${expTan.toDouble}(${expTan.litValue})"
        )
      }
    }
  }

  it should "correctly compute csc and cot for angles with non-zero sine" in {
    val maxError = 2
    simulate(new TrigLUT) { dut =>
      for (angle <- testAnglesNoSinZero) {
        val rawAngle = toFP(angle)
        dut.io.angle.poke(rawAngle)
        dut.clock.step()

        // cosecant
        val expCsc = toFP(1.0 / math.sin(angle))
        val gotCsc = dut.io.csc.peek()
        assert(
          (gotCsc.toDouble - expCsc.toDouble).abs < maxError,
          s"csc mismatch for angle $angle: got ${gotCsc.toDouble}(${gotCsc.litValue}), expected ${expCsc.toDouble}(${expCsc.litValue})"
        )

        // cotangent
        val expCot = toFP(math.cos(angle) / math.sin(angle))
        val gotCot = dut.io.cot.peek()
        assert(
          (gotCot.toDouble - expCot.toDouble).abs < maxError,
          s"cot mismatch for angle $angle: got ${gotCot.toDouble}(${gotCot.litValue}), expected ${expCot.toDouble}(${expCot.litValue})"
        )
      }
    }
  }
}
