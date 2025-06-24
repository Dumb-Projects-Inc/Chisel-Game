package gameEngine.entity

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class SpritePlacerSimpleSpec extends AnyFunSpec with ChiselSim with Matchers {

  it("Look from left to right") {
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
      dut.io.input.bits.playerPos.x.poke(toFP(1.0))
      dut.io.input.bits.playerPos.y.poke(toFP(1.0))

      dut.io.output.ready.poke(true.B)

      val startAngle = math.Pi / 2
      val endAngle = 0.0
      val step = 0.05

      var angle = startAngle
      while (angle >= endAngle) {
        dut.io.input.valid.poke(true.B)
        dut.io.input.bits.playerAngle.poke(toFP(angle))
        dut.clock.step()
        dut.io.input.valid.poke(false.B)

        for (_ <- 0 until 30) {
          if (dut.io.output.valid.peek().litToBoolean) {
            val visible = dut.io.output.bits.visible.peek().litToBoolean
            val xOffset = dut.io.output.bits.xOffset.peek().litValue
            val invLen = dut.io.output.bits.invLen.peek().litValue
            println(f"angle = $angle%1.2f | visible = $visible | xOffset = $xOffset | invLen = $invLen")
          }
          dut.clock.step()
        }

        angle -= step
      }
    }
  }
}
