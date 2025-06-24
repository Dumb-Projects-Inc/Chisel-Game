package gameEngine.util

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import scala.collection.mutable.ArrayBuffer
import gameEngine.fixed.FixedPointUtils
class PosExchangeSpec extends AnyFunSpec with ChiselSim with Matchers {
  it("round-trips a Vec2 over UART") {
    simulate(new posExchange) { dut =>
      // === INITIALIZE ALL INPUT SINKS ===
      dut.io.playerPos.valid.poke(false.B)
      dut.io.playerPos.bits.x.poke(0.S)
      dut.io.playerPos.bits.y.poke(0.S)
      dut.io.out.ready.poke(false.B)
      dut.io.in.valid.poke(false.B)
      dut.io.in.bits.poke(0.U)
      dut.io.bobPos.ready.poke(false.B)
      dut.io.bobPos.valid.poke(true.B)
      dut.clock.step() // settle

      // === SET UP HANDSHAKE FOR A SINGLE TRANSFER ===
      val xFP = toFP(1.5) // SInt fixed-point
      val yFP = toFP(-0.5)
      val xExp = BigInt(xFP.litValue.toLong)
      val yExp = BigInt(yFP.litValue.toLong)

      // enable out ready so send can proceed
      dut.io.out.ready.poke(true.B)
      // pulse playerPos.valid/bits
      dut.io.playerPos.bits.x.poke(xFP)
      dut.io.playerPos.bits.y.poke(yFP)
      dut.io.playerPos.valid.poke(true.B)
      dut.clock.step()
      dut.io.playerPos.valid.poke(false.B)

      // collect the outgoing bytes
      val W = FixedPointUtils.width
      val TOTAL_BITS = 2 * W
      val nBytes = (TOTAL_BITS + 7) / 8
      val sent = ArrayBuffer[BigInt]()
      while (sent.size < nBytes) {
        if (dut.io.out.valid.peek().litToBoolean) {
          sent += dut.io.out.bits.peek().litValue
        }
        dut.clock.step()
      }

      // prepare to accept bobPos
      dut.io.bobPos.ready.poke(true.B)

      // feed them back into `in`
      var idx = 0
      while (!dut.io.bobPos.valid.peek().litToBoolean) {
        if (idx < sent.size) {
          dut.io.in.bits.poke(sent(idx).U)
          dut.io.in.valid.poke(true.B)
        } else {
          dut.io.in.valid.poke(false.B)
        }
        dut.clock.step()
        if (
          dut.io.in.valid.peek().litToBoolean && dut.io.in.ready
            .peek()
            .litToBoolean
        ) {
          idx += 1
        }
      }

      // finally check results
      val outX = dut.io.bobPos.bits.x.peek().litValue
      val outY = dut.io.bobPos.bits.y.peek().litValue
      outX shouldEqual xExp
      outY shouldEqual yExp
    }
  }
}
