package gameEngine.screen

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim

class VGATimingSpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("VGATiming for 640*480 @ 60hz") {
    import gameEngine.screen.VgaConfigs.vga640x480._
    it("outputs correct hsync") {
      simulate(new VGATiming) { dut =>
        dut.io.hSync.expect(false.B, "hSync should start low")
        dut.io.vSync.expect(false.B, "vSync should start low")
        dut.io.visible.expect(true.B, "visible should start high")
        dut.io.pixelX.expect(0, "We should start at (0,0)")
        dut.io.pixelY.expect(0, "We should start at (0,0)")

        dut.clock.step((visibleAreaH * 4) - 1)
        dut.io.visible.expect(true.B)
        dut.clock.step()
        dut.io.visible.expect(false.B)

        dut.clock.step((frontPorchH * 4) - 1)
        dut.io.hSync.expect(false.B)
        dut.clock.step()
        dut.io.hSync.expect(true.B, "hSync should be pulled high")

        dut.clock.step((syncPulseH * 4) - 1)
        dut.io.hSync.expect(true.B)
        dut.clock.step()
        dut.io.hSync.expect(false.B, "hSync should be pulled low again")

        dut.clock.step((backPorchH * 4) - 1)
        dut.io.visible.expect(false.B)
        dut.clock.step()
        dut.io.visible.expect(true.B, "visible should be pulled high")

      }
    }

    it("outputs correct vsync") {
      simulate(new VGATiming) { dut =>
        dut.io.hSync.expect(false.B, "hSync should start low")
        dut.io.vSync.expect(false.B, "vSync should start low")
        dut.io.visible.expect(true.B, "visible should start high")
        dut.io.pixelX.expect(0, "We should start at (0,0)")
        dut.io.pixelY.expect(0, "We should start at (0,0)")

        dut.clock.step((dut.totalH * (visibleAreaV + frontPorchV) * 4) - 1)
        dut.io.vSync.expect(false.B)
        dut.clock.step()
        dut.io.vSync.expect(true.B, "vSync is pulled high")

        dut.clock.step((dut.totalH * syncPulseV * 4) - 1)
        dut.io.vSync.expect(true.B)
        dut.clock.step()
        dut.io.vSync.expect(false.B, "vSync is pulled low")

      }
    }

  }
}
