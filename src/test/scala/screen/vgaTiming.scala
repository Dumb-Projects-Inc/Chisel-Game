package gameEngine.screen

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.EphemeralSimulator._

class VGATimingSpec extends AnyFlatSpec {
  behavior of "VGATiming"
  it should "output correct hsync and vsync" in {
    simulate(new VGATiming) { dut =>
      // reset
      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.reset.poke(false.B)

      // Initial state (Front Porch)
      dut.io.hSync.expect(false.B)
      dut.io.vSync.expect(false.B)
      dut.io.visible.expect(true.B)

      // Step through the Horizontal front porch
      dut.clock.step(dut.startHSync + 1)
      dut.io.hSync.expect(true.B)
      dut.io.vSync.expect(false.B)
      dut.io.visible.expect(false.B)

      while (dut.io.visible.peek().litValue == 0) {
        dut.clock.step()
      }
      // Check visible area
      dut.io.hSync.expect(false.B)
      dut.io.vSync.expect(false.B)
      dut.io.visible.expect(true.B)
      dut.io.pixelX.expect(0.U)
      dut.io.pixelY.expect(1.U)

      // Check that the pixelX and pixelY are incrementing
      while (
        dut.io.pixelY.peek().litValue == 1 || dut.io.pixelY.peek().litValue == 0
      ) {
        dut.clock.step()
      }
      dut.io.pixelX.expect(0.U)
      dut.io.pixelY.expect(2.U)

    }
  }
}
