package gameEngine

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class EngineSpec extends AnyFlatSpec  {
  behavior of "Engine"

  it should "go through a pixelClock cycle" in {
    simulate(new Engine) { e =>
      e.io.pixelClock.poke(true.B)
      e.clock.step(1)
      e.io.pixelClock.expect(true.B)
      e.io.pixelClock.poke(false.B)
      e.clock.step(1)
      e.io.pixelClock.expect(false.B)
      val pixelClockEndVal = e.io.pixelClock.peek().litValue
      println(s"pixelClock ends on false: $pixelClockEndVal")
    }
  }
}
