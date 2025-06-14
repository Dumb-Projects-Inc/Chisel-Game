package gameEngine.fixed

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.InverseSqrt
import gameEngine.fixed.FixedPointUtils._

class SqrtSpec extends AnyFlatSpec {
  behavior of "Sqrt"

  val errormargin = 0.2
  def checkMargin(actual: Double, expected: Double): Boolean = {
    val diff = Math.abs(actual - expected)
    diff <= errormargin
  }

  it should "compute the inverse sqrt" in {
    val cases = Seq(
      1, 2, 1.5, 0.5, 0.25, 0.125, 20
      // 0-300
    )
    simulate(new InverseSqrt) { dut =>
      for (input <- cases) {
        dut.clock.step()
        dut.io.input.poke(toFP(input))
        dut.io.wrEn.poke(true.B)
        dut.clock.step()
        dut.io.wrEn.poke(false.B)

        dut.clock.step(10)

        val output = fromFP(dut.io.output.peek())
        dut.io.ready.expect(true.B)
        assert(
          checkMargin(output, 1.0 / Math.sqrt(input)),
          s"Failed for input $input: got $output expected ${1.0 / Math.sqrt(input)}"
        )
      }
    }
  }
  it should "handle random cases" in {
    val random = new scala.util.Random(42) // Fixed seed
    val cases = Seq.fill(200)(
      random.nextDouble() * 100 + 1
    ) // Random inputs between 1 and 300

    simulate(new InverseSqrt) { dut =>
      for (input <- cases) {
        dut.io.input.poke(toFP(input))
        dut.io.wrEn.poke(true.B)
        dut.clock.step()
        dut.io.wrEn.poke(false.B)

        dut.clock.step(10)

        val output = fromFP(dut.io.output.peek())
        val output = fromFP(dut.io.output.peek())
        assert(
          checkMargin(output, 1.0 / Math.sqrt(input)),
          s"Failed for input $input: got $output, expected ${1.0 / Math.sqrt(input)}"
        )
        print("Clocks taken: " + counter + "\n")
      }
    }
  }

}
