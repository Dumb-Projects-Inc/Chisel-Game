package gameEngine.screen

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FrameBufferSpec extends AnyFlatSpec with Matchers {
  behavior of "FrameBuffer"

  it should "read and write values correctly" in {
    simulate(new FrameBuffer) { fb =>
      // Setting random address to write to
      fb.io.address.poke(10.U)
      // Set random value to write
      fb.io.wrData.poke(123.U)
      // Enables write option
      fb.io.wrEn.poke(true.B)

      fb.clock.step(1)
      
      // Expect to see 0 in data since the framebufSel is false as
      // default and therefore the write is done to framebuf1 but
      // the read is done from framebuf0
      fb.io.data.expect(0.U)

      // Switch the framebufSel to true so that the write is done 
      // to framebuf0 and the read is done from framebuf1 next step
      fb.io.switch.poke(true.B)

      fb.clock.step(1)

      // Expect to see 123 now that the framebufSel is true
      fb.io.data.expect(123.U)
      val readVal = fb.io.data.peek().litValue
      println(s"Read from address 10: $readVal")
    }
  }
}
