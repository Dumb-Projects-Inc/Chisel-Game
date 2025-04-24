package gameEngine.screen

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.EphemeralSimulator._

class VGAControllerSpec extends AnyFlatSpec {
  behavior of "VGAController"
  it should "output correct x and y" in {
    simulate(new VGAController(noBlackBox = true)) { dut =>
        // NOTE: The simulation time is not accurate, as the clock is not running at the correct frequency.

        // Initial state
        dut.io.x.expect(0.U)
        dut.io.y.expect(0.U)
        
        // Step until x changes
        var xClocks = 0
        while (dut.io.x.peek().litValue == 0) {
            dut.clock.step(1)
            xClocks += 1
        }

        // Check x value
        dut.io.x.expect(1.U)
        dut.io.y.expect(0.U)

        // Step until y changes
        while (dut.io.y.peek().litValue == 0) {
            dut.clock.step(xClocks)
        }

        // Check y value
        dut.io.x.expect(0.U)
        dut.io.y.expect(1.U)


    }
  }

  it should "output correct colors from input" in {
    simulate(new VGAController(noBlackBox = true)) { dut =>
        // Initial state
        dut.io.vga.red.expect(0.U)
        dut.io.vga.green.expect(0.U)
        dut.io.vga.blue.expect(0.U)

        // Set pixel color to white
        dut.io.pixel.poke("b111111111111".U)
        dut.clock.step(1)

        // Check output colors
        dut.io.vga.red.expect(15.U) 
        dut.io.vga.green.expect(15.U) 
        dut.io.vga.blue.expect(15.U)

    }
  }
}
