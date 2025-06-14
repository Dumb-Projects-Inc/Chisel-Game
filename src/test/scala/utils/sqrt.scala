package gameEngine.fixed

import org.scalatest.funspec.AnyFunSpec
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import gameEngine.fixed.InverseSqrt
import gameEngine.fixed.FixedPointUtils._
import org.scalatest.matchers.should.Matchers

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
    val cases = Seq.fill(200)(
      random.nextDouble() * 64
    ) // Random inputs between 1 and 300

    simulate(new InverseSqrt) { dut =>
      for (input <- cases) {
        withClue(input.toString() + "\n") {
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
