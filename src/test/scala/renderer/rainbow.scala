package gameEngine.renderer

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._

class RainbowRendererSpec extends AnyFlatSpec {
  behavior of "RainbowRenderer"
  it should "Render a Rainbow" in {
    simulate(new RainbowRenderer) { dut =>
      // Initial state
      dut.io.x.poke(0.U)
      dut.io.y.poke(0.U)

      // First Region (0)
      dut.io.resultPixel.expect(0xf00.U)
      dut.io.x.poke(1.U)
      dut.io.y.poke(0.U)
      dut.io.resultPixel.expect(0xf00.U)

      // Next region (1)
      dut.io.x.poke(43.U)
      dut.io.y.poke(0.U)
      dut.io.resultPixel.expect(0xff0.U)

      // Next region (2)
      dut.io.x.poke(43.U)
      dut.io.y.poke(43.U)
      dut.io.resultPixel.expect(0x0f0.U)

      // Next region (3)
      dut.io.x.poke(86.U)
      dut.io.y.poke(43.U)
      dut.io.resultPixel.expect(0x0ff.U)

      // Next region (4)
      dut.io.x.poke(129.U)
      dut.io.y.poke(43.U)
      dut.io.resultPixel.expect(0x00f.U)

      // Next region (5)
      dut.io.x.poke(172.U)
      dut.io.y.poke(43.U)
      dut.io.resultPixel.expect(0xf0f.U)
    }
  }
}
