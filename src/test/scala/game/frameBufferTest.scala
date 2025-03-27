package gameEngine.screen

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FrameBufferSpec extends AnyFlatSpec with Matchers {
  behavior of "FrameBuffer"

  it should "read and write values correctly" in {
    simulate(new FrameBuffer) { fb =>
      fb.io.address.poke(10.U)
      fb.io.wrData.poke(123.U)
      fb.io.wrEn.poke(true.B)

      fb.clock.step(1)
      
      fb.io.data.expect(0.U)
      fb.io.switch.poke(true.B)

      fb.clock.step(1)

      fb.io.data.expect(123.U)
      val readVal = fb.io.data.peek().litValue
      println(s"Read from address 10: $readVal")
    }
  }
}
