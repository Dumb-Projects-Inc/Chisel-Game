package gameEngine.entity.library

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import chisel3._
import chisel3.simulator.scalatest.ChiselSim

class ParallelogramSpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("ParallelogramSpec") {

    it("correctly determines if we are inside parallelogram or not") {
      simulate(new Parallelogram) { dut =>
        dut.io.p0.x.poke(220)
        dut.io.p0.y.poke(75)
        dut.io.p1.x.poke(200)
        dut.io.p1.y.poke(80)
        dut.io.p2.x.poke(220)
        dut.io.p2.y.poke(200)
        dut.io.update.poke(true)
        dut.io.point.x.poke(0)
        dut.io.point.y.poke(0)
        dut.clock.step()
        dut.io.update.poke(false)

        dut.io.point.x.poke(210)
        dut.io.point.y.poke(180)
        dut.clock.step()

        dut.io.point.x.poke(40)
        dut.io.point.y.poke(40)
        dut.clock.step()

        dut.io.point.x.poke(205)
        dut.io.point.y.poke(80)
        dut.clock.step()

        dut.io.point.x.poke(205)
        dut.io.point.y.poke(80)
        dut.clock.step(5)
      }
    }
  }
}
