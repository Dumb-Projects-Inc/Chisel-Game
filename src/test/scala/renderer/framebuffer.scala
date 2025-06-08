package gameEngine.framebuffer

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim

class FrameBufferSpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("FrameBuffer") {

    it("should read back written value") {
      simulate(new FrameBuffer(16, 16, 2)) { dut =>
        dut.io.enable.poke(true.B)
        dut.io.write.poke(true.B)
        dut.io.x.poke(5.U)
        dut.io.y.poke(7.U)
        dut.io.dataIn.poke(2.U)
        dut.clock.step()

        dut.io.write.poke(false.B)
        dut.clock.step()
        dut.io.dataOut.expect(2.U)

        dut.clock.step()
        dut.io.dataOut.expect(2.U)
      }
    }

    it("should only update its own index") {
      simulate(new FrameBuffer(16, 16, 8)) { dut =>
        dut.io.enable.poke(true.B)
        dut.io.write.poke(true.B)
        for (i <- 0 to 15; j <- 0 to 15) {
          dut.io.x.poke(i.U)
          dut.io.y.poke(j.U)
          dut.io.dataIn.poke(i * j)
          dut.clock.step()
        }

        dut.io.write.poke(false.B)

        for (i <- 15 to 0; j <- 15 to 0) {
          dut.io.x.poke(i.U)
          dut.io.y.poke(j.U)
          dut.clock.step()
          dut.io.dataOut.expect(i * j)
        }
      }
    }

  }
}
