package gameEngine.fixed

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.InverseSqrt
import gameEngine.fixed.FixedPointUtils._

class SqrtSpec extends AnyFlatSpec {
  behavior of "Sqrt"

  val errormargin = 0.1
  def checkMargin(actual: Double, expected: Double): Boolean = {
    val diff = Math.abs(actual - expected)
    diff <= errormargin
  }

  it should "compute the inverse sqrt" in {
    val cases = Seq(
      1, 2, 1.5, 0.5, 0.25, 0.125, 20
      // 0-300
    )
    simulate(new InverseSqrt(errormargin)) { dut =>
      for (input <- cases) {
        dut.io.input.poke(toFP(input))
        dut.io.wrEn.poke(true.B)
        dut.clock.step()
        dut.io.wrEn.poke(false.B)

        var done = false
        var counter = 0
        while (!done) {
          counter += 1
          dut.clock.step()
          done = dut.io.ready.peek().litToBoolean
        }

        val output = fromFP(dut.io.output.peek())
        assert(
          checkMargin(output, 1.0 / Math.sqrt(input)),
          s"Failed for input $input: got $output"
        )
        print("Clocks taken: " + counter + "\n")
      }
    }
  }
  it should "handle random cases" in {
    val random = new scala.util.Random(42) // Fixed seed
    val cases = Seq.fill(100)(
      random.nextDouble() * 300 + 1
    ) // Random inputs between 1 and 300

    simulate(new InverseSqrt(errormargin)) { dut =>
      for (input <- cases) {
        dut.io.input.poke(toFP(input))
        dut.io.wrEn.poke(true.B)
        dut.clock.step()
        dut.io.wrEn.poke(false.B)

        var done = false
        var counter = 0
        while (!done) {
          counter += 1
          dut.clock.step()
          done = dut.io.ready.peek().litToBoolean
        }

        val output = fromFP(dut.io.output.peek())
        assert(
          checkMargin(output, 1.0 / Math.sqrt(input)),
          s"Failed for input $input: got $output"
        )
        print("Clocks taken: " + counter + "\n")
      }
    }
  }

}
