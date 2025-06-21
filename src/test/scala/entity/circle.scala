package gameEngine.entity

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim

import gameEngine.entity.CircleEntity
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._
class CircleEntitySpec extends AnyFunSpec with ChiselSim with Matchers {
  it("should calculate visibility") {

    simulate(new CircleEntity) { dut =>
      dut.io.input.bits.pos.x.poke(toFP(1.0))
      dut.io.input.bits.pos.y.poke(toFP(1.0))
      dut.io.input.bits.playerPos.x.poke(toFP(0.0))
      dut.io.input.bits.playerPos.y.poke(toFP(0.0))
      dut.io.input.bits.playerAngle.poke(toFP(math.Pi / 4.0))
      dut.io.input.valid.poke(true.B)
      dut.clock.stepUntil(dut.io.output.valid, 1, 100)

      dut.io.output.bits.visible.expect(true.B)
    }
  }

}
