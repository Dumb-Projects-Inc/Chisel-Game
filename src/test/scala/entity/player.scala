package gameEngine.entity

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim

import gameEngine.entity.PlayerEntity
import gameEngine.fixed.FixedPointUtils._

class PlayerEntitySpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("PlayerEntity") {
    it("should rotate correctly") {
      simulate(new PlayerEntity(0.0, 0.0, 0.0, Seq(Seq(1, 1, 1)))) { dut =>
        dut.io.action.poke(PlayerAction.turn)
        // mov 4/pi
        dut.io.actionArg.poke(toFP(math.Pi / 4.0))
        dut.clock.step()

        dut.io.angle.expect(toFP(math.Pi / 4.0))
      }
    }

  }
}
