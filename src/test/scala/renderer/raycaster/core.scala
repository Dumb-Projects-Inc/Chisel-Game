package gameEngine.raycast

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2._

class RaycasterCoreSpec extends AnyFunSpec with ChiselSim with Matchers {

  val testMap = Seq(
    Seq(1, 1, 1),
    Seq(1, 0, 1),
    Seq(1, 2, 1)
  )

  describe("RaycasterCore") {
    it("quick smoke test") {
      simulate(
        new RaycasterCore(
          map = testMap,
          nTiles = 3,
          width = 320,
          height = 20,
          fov = 0.2
        )
      ) { dut =>
        dut.io.in.ready.expect(true.B)
        dut.io.in.bits.start.x.poke(toFP(1.5))
        dut.io.in.bits.start.y.poke(toFP(1.5))
        dut.io.in.bits.angle.poke(toFP(math.Pi / 2))
        dut.io.in.valid.poke(true.B)
        dut.clock.step()
        dut.io.in.valid.poke(false.B)

        // wait for the columns to appear
        dut.clock.stepUntil(dut.io.columns.valid, 1, 5000)
        dut.io.columns.valid.expect(true.B)

        // every column should be clamped to height=20, tile=1, horizontal hit
        for (i <- 0 until 320) {
          dut.io.columns.bits(i).height.peek().litValue should be(20)
          dut.io.columns.bits(i).tile.peek().litValue should be(2)
          dut.io.columns.bits(i).isHorizontal.peek().litToBoolean should be(
            true
          )
        }
      }
    }
  }
}
