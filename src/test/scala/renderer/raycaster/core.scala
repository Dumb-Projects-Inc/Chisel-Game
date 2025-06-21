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
    Seq(1, 5, 1)
  )

  describe("RaycasterCore") {
    it("quick smoke test") {
      simulate(
        new RaycasterCore(
          map = testMap,
          width = 3,
          height = 240,
          fov = 2
        )
      ) { dut =>
        dut.io.in.ready.expect(true.B)
        dut.io.in.bits.start.x.poke(toFP(1.1))
        dut.io.in.bits.start.y.poke(toFP(1.1))
        dut.io.in.bits.angle.poke(toFP(math.Pi / 2))
        dut.io.in.valid.poke(true.B)
        dut.clock.step()
        dut.io.in.valid.poke(false.B)

        // wait for the columns to appear
        dut.clock.stepUntil(dut.io.columns.valid, 1, 1000)
        dut.io.columns.valid.expect(true.B)

        dut.io.columns.bits(0).fracHit.peek().litValue.toInt should be(
          2775 +- 5 // 2775 / 2^12 = 0.667
        )
        dut.io.columns.bits(0).isHorizontal.peek().litToBoolean shouldBe false
        dut.io.columns.bits(0).tile.peek().litValue shouldBe 1
        dut.io.columns.bits(0).height.peek().litValue.toInt should be(221 +- 2)

        dut.io.columns.bits(1).fracHit.peek().litValue.toInt should be(
          390 +- 5
        )
        dut.io.columns.bits(1).isHorizontal.peek().litToBoolean shouldBe true
        dut.io.columns.bits(1).tile.peek().litValue shouldBe 5
        dut.io.columns.bits(1).height.peek().litValue.toInt should be(140 +- 2)

        dut.io.columns.bits(2).fracHit.peek().litValue.toInt should be(
          670 +- 5
        )
        dut.io.columns.bits(2).isHorizontal.peek().litToBoolean shouldBe false
        dut.io.columns.bits(2).tile.peek().litValue shouldBe 1
        dut.io.columns.bits(2).height.peek().litValue.toInt should be(240)

      }
    }
  }
}
