package gameEngine.fixed

import org.scalatest.funspec.AnyFunSpec
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import gameEngine.fixed.InverseSqrt
import gameEngine.fixed.FixedPointUtils._
import org.scalatest.matchers.should.Matchers

import chisel3._
import chisel3.util._
import scala.math._

/** Inverse square root using piecewise-linear LUT approximation, implemented
  * with SInt fixed-point format.
  */

class SqrtSpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("Sqrt") {
    it("compute the inverse sqrt") {
      val cases = Seq(
        1, 2, 1.5, 0.5, 0.25, 0.125, 20
        // 0-300
      )
      simulate(new InverseSqrt) { dut =>
        for (input <- cases) {
          withClue(input) {
            dut.io.input.ready.expect(true.B)
            dut.io.input.bits.poke(toFP(input))
            dut.io.input.valid.poke(true.B)
            dut.clock.stepUntil(dut.io.result.valid, 1, 100)

            dut.io.result.valid.expect(true.B)
            val result = dut.io.result.bits.peek().toDouble
            result should be(1.0 / math.sqrt(input) +- 0.1)
            dut.io.result.ready.poke(true.B)
            dut.clock.step()
          }
        }
      }
    }
  }
  it("handle random cases") {
    val random = new scala.util.Random(42) // Fixed seed
    val generalCases = Seq.fill(200)(
      random.nextDouble() * 1024
    )
    val smallCases = Seq.fill(100)(
      random.nextDouble() * 10
    )

    val cases = (generalCases ++ smallCases).distinct

    var sumError = 0.0
    var caseCount = 0

    simulate(new InverseSqrt) { dut =>
      for (input <- cases) {
        withClue(input.toString() + "\n") {
          dut.io.input.ready.expect(true.B)
          dut.io.input.bits.poke(toFP(input))
          dut.io.input.valid.poke(true.B)
          dut.clock.stepUntil(dut.io.result.valid, 1, 100)

          dut.io.result.valid.expect(true.B)
          val result = dut.io.result.bits.peek().toDouble
          val ideal = 1.0 / math.sqrt(input)
          val absError = abs(result - ideal)
          val pctError = absError / ideal * 100.0
          sumError += pctError
          caseCount += 1

          absError should be < 0.1

          dut.io.result.ready.poke(true.B)
          dut.clock.step()
        }
      }
    }

    val avgPctError = sumError / caseCount
    info(f"Ran $caseCount tests, average percent error = $avgPctError%.4f%%")
  }

}
