package gameEngine.util

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import scala.collection.mutable.ArrayBuffer

class PosExchangeSimpleSpec extends AnyFunSpec with ChiselSim with Matchers {
  it("exchanges a single Vec2 over UART") {
    simulate(new posExchange) { dut =>
      // drive ready for send
      dut.io.out.ready.poke(true.B)
      // push one playerPos
      val xIn = toFP(3.5)
      val yIn = toFP(-1.25)
      dut.io.playerPos.bits.x.poke(xIn)
      dut.io.playerPos.bits.y.poke(yIn)
      dut.io.playerPos.valid.poke(true.B)
      dut.clock.step()
      dut.io.playerPos.valid.poke(false.B)

      val rxBytes = ArrayBuffer[Int]()
      var recvIdx = 0
      // loop until bobPos appears
      while (!dut.io.bobPos.valid.peek().litToBoolean) {
        // collect outgoing bytes
        if (dut.io.out.valid.peek().litToBoolean) {
          rxBytes += dut.io.out.bits.peek().litValue.toInt
        }
        // when ready, feed them back in
        if (dut.io.in.ready.peek().litToBoolean && recvIdx < rxBytes.size) {
          dut.io.in.valid.poke(true.B)
          dut.io.in.bits.poke(rxBytes(recvIdx).U)
          recvIdx += 1
        } else {
          dut.io.in.valid.poke(false.B)
        }
        dut.clock.step()
      }

      // once valid, sample bobPos
      val W = 24 // Set this to the correct fixed-point width used in your design
      val xOut = dut.io.bobPos.bits.x.peek().litValue.toDouble / (1 << (W-1)) // adjust for fixed point
      val yOut = dut.io.bobPos.bits.y.peek().litValue.toDouble / (1 << (W-1))
      println(f"received bobPos.x = $xOut%.3f, bobPos.y = $yOut%.3f")
    }
  }
}
