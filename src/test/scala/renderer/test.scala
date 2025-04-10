package gameEngine.renderer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.EphemeralSimulator._

class LinebufferSpec extends AnyFlatSpec with Matchers {

  "lineBuffer" should "correctly switch on rising edge" in {
    simulate(new LineBuffer(10)) { dut => 
      // Initial state
      dut.bufferSel.expect(false.B)
      dut.io.data.expect(0.U)

      // Write to buffer
      dut.io.wrAddr.poke(1.U)
      dut.io.wrData.poke(5.U)
      dut.clock.step()
      dut.io.data.expect(0.U) // No read yet

      // Switch to buffer 1
      dut.io.switch.poke(true.B)
      dut.clock.step()
      dut.bufferSel.expect(true.B)
      dut.clock.step()
      dut.bufferSel.expect(true.B) // Ensure buffer 1 is selected 2 cycles later

      // Write to buffer
      dut.io.wrAddr.poke(1.U)

      dut.io.wrData.poke(10.U)
      dut.clock.step()

      // Read from buffer 0 (should still be 5)
      dut.io.address.poke(1.U)
      dut.clock.step()
      dut.io.data.expect(5.U)

      // Read from buffer 1 (should be 10)
      dut.io.switch.poke(true.B)
      dut.clock.step(2)
      dut.io.data.expect(10.U)    
    }
  }
}