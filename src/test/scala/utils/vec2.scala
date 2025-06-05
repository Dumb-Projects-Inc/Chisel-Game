package gameEngine.vec2

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import gameEngine.vec2.Vec2._

class Tester extends Module {
  val io = IO(
    new Bundle {
      val x = Input(UInt(12.W))
      val y = Input(UInt(12.W))
      val outVec2 = Output(new Vec2(UInt(12.W)))
      val outVec = Output(new Vec2(SInt(12.W)))
    }
  )

  val test = RegInit(Vec2(2.S(12.W), 2.S(12.W)))
  val test2 = RegInit(Vec2(1.S(12.W), 1.S(12.W)))

  io.outVec := test + test2
  io.outVec2 := Vec2(io.x, io.y) + Vec2(io.x, io.y)

}

class Vec2Spec extends AnyFlatSpec with ChiselSim with Matchers {
  "Vec2" should "create a named bundle of data" in {
    simulate(new Tester) { dut =>
      dut.io.x.poke(8)
      dut.io.y.poke(12)

      dut.clock.step()

      dut.io.outVec.x.expect(3)
      dut.io.outVec.y.expect(3)
      dut.io.outVec2.x.expect(16)
      dut.io.outVec2.y.expect(24)
    }
  }
}
