package gameEngine.entity

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim

import gameEngine.entity.SpriteEntity
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._
class SpritePlacerSpec extends AnyFunSpec with ChiselSim with Matchers {
  it("should calculate visibility") {
    val map = Seq(
      Seq(1, 1, 1, 1, 1, 1, 1, 1, 1),
      Seq(1, 0, 0, 0, 0, 0, 0, 0, 1),
      Seq(1, 0, 0, 0, 0, 0, 0, 0, 1),
      Seq(1, 0, 0, 0, 0, 0, 0, 0, 1),
      Seq(1, 1, 1, 1, 1, 1, 1, 1, 1)
    )
    simulate(new SpritePlacer(map = map)) { dut =>
      dut.io.input.bits.pos.x.poke(toFP(2.0))
      dut.io.input.bits.pos.y.poke(toFP(2.0))
      dut.io.input.bits.playerPos.x.poke(toFP(1.5))
      dut.io.input.bits.playerPos.y.poke(toFP(1.5))
      dut.io.input.bits.playerAngle.poke(toFP(math.Pi / 4.0))
      dut.io.input.valid.poke(true.B)
      dut.clock.step()
      dut.io.input.valid.poke(false.B)
      dut.clock.stepUntil(dut.io.output.valid, 1, 100)
      dut.io.output.bits.visible.expect(true.B)

      dut.io.output.ready.poke(true.B)
      dut.clock.step()
      dut.io.output.ready.poke(false.B)
      dut.io.input.bits.playerAngle.poke(toFP(3 * math.Pi / 4.0))
      dut.io.input.valid.poke(true.B)
      dut.clock.stepUntil(dut.io.output.valid, 1, 100)
      dut.io.output.bits.visible.expect(false.B)

    }
  }

  it("should not be visible through a wall") {
    val map = Seq(
      Seq(1, 1, 1, 1, 1, 1, 1, 1, 1),
      Seq(1, 0, 0, 0, 1, 0, 0, 0, 1),
      Seq(1, 0, 0, 0, 1, 0, 0, 0, 1),
      Seq(1, 0, 0, 0, 1, 0, 0, 0, 1),
      Seq(1, 1, 1, 1, 1, 1, 1, 1, 1)
    )
    simulate(new SpritePlacer(map = map)) { dut =>
      dut.io.input.bits.pos.x.poke(toFP(6.5))
      dut.io.input.bits.pos.y.poke(toFP(2.5))
      dut.io.input.bits.playerPos.x.poke(toFP(2.5))
      dut.io.input.bits.playerPos.y.poke(toFP(2.5))
      dut.io.input.bits.playerAngle.poke(toFP(0))
      dut.io.input.valid.poke(true.B)
      dut.clock.step()
      dut.io.input.valid.poke(false.B)
      dut.clock.stepUntil(dut.io.output.valid, 1, 100)
      dut.io.output.bits.visible.expect(false.B)

      dut.io.output.ready.poke(true.B)
      dut.clock.stepUntil(dut.io.input.ready, 1, 100)
      dut.io.output.ready.poke(false.B)
      dut.io.input.bits.pos.x.poke(toFP(3.5))
      dut.io.input.bits.pos.y.poke(toFP(2.5))
      dut.io.input.valid.poke(true.B)
      dut.clock.stepUntil(dut.io.output.valid, 1, 100)
      dut.io.output.bits.visible.expect(true.B)
    }
  }

}
