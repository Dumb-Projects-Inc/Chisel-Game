package gameEngine.framebuffer

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import scala.util.Random

class BufferSpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("Buffer") {

    it("should read back written value") {
      simulate(new Buffer(2)) { dut =>
        // write a single element and read it back
        dut.io.enable.poke(true.B)
        dut.io.write.poke(true.B)
        dut.io.x.poke(5.U)
        dut.io.y.poke(7.U)
        dut.io.dataIn.poke(2.U)
        dut.clock.step()

        // stop writing, then read
        dut.io.write.poke(false.B)
        dut.clock.step()
        dut.io.dataOut.expect(2.U)

        // remain stable over further cycles
        dut.clock.step()
        dut.io.dataOut.expect(2.U)
      }
    }

    it("should only update its own index and preserve others") {
      simulate(new Buffer(8)) { dut =>
        // initialize all locations to zero
        dut.io.enable.poke(true.B)
        dut.io.write.poke(false.B)
        for (i <- 0 until 8; j <- 0 until 8) {
          dut.io.x.poke(i.U)
          dut.io.y.poke(j.U)
          dut.clock.step()
          dut.io.dataOut.expect(0.U)
        }

        // write a unique pattern across a subset
        dut.io.write.poke(true.B)
        for (i <- 0 until 8; j <- 0 until 8) {
          dut.io.x.poke(i.U)
          dut.io.y.poke(j.U)
          dut.io.dataIn.poke((i * 16 + j).U)
          dut.clock.step()
        }

        // read back and check
        dut.io.write.poke(false.B)
        for (i <- 0 until 8; j <- 0 until 8) {
          dut.io.x.poke(i.U)
          dut.io.y.poke(j.U)
          dut.clock.step()
          dut.io.dataOut.expect((i * 16 + j).U)
        }
      }
    }

    it("should not change memory when enable is false") {
      simulate(new Buffer(4)) { dut =>
        // write something with enable active
        dut.io.enable.poke(true.B)
        dut.io.write.poke(true.B)
        dut.io.x.poke(1.U)
        dut.io.y.poke(1.U)
        dut.io.dataIn.poke(7.U)
        dut.clock.step()

        // disable module and attempt write
        dut.io.enable.poke(false.B)
        dut.io.write.poke(true.B)
        dut.io.dataIn.poke(3.U)
        dut.clock.step()

        // re-enable and read back old value
        dut.io.enable.poke(true.B)
        dut.io.write.poke(false.B)
        dut.clock.step()
        dut.io.dataOut.expect(7.U)
      }
    }

    it("should reflect write-first behavior immediately on output") {
      simulate(new Buffer(8)) { dut =>
        dut.io.enable.poke(true.B)
        dut.io.write.poke(true.B)
        dut.io.x.poke(2.U)
        dut.io.y.poke(3.U)
        dut.io.dataIn.poke(15.U)
        dut.clock.step()
        // in write-first mode, dataOut should equal dataIn of same cycle
        dut.io.dataOut.expect(15.U)
      }
    }

    it("should handle randomized writes and reads") {
      simulate(new Buffer(8)) { dut =>
        val rand = new Random(0)
        val expected = scala.collection.mutable.Map[(Int, Int), BigInt]()
        dut.io.enable.poke(true.B)

        // perform random writes within 8x8 region
        dut.io.write.poke(true.B)
        for (_ <- 0 until 50) {
          val x = rand.nextInt(8)
          val y = rand.nextInt(8)
          val data = rand.nextInt(1 << 8)
          expected((x, y)) = BigInt(data)
          dut.io.x.poke(x.U)
          dut.io.y.poke(y.U)
          dut.io.dataIn.poke(data.U)
          dut.clock.step()
        }

        // switch to read mode and verify a sample of locations
        dut.io.write.poke(false.B)
        val sample = rand.shuffle(expected.keys.toList).take(20)
        for ((x, y) <- sample) {
          dut.io.x.poke(x.U)
          dut.io.y.poke(y.U)
          dut.clock.step()
          val exp = expected((x, y)).U
          dut.io.dataOut.expect(exp)
        }
      }
    }

    it("should support boundary coordinates without overflow") {
      simulate(new Buffer(8)) { dut =>
        dut.io.enable.poke(true.B)
        dut.io.write.poke(true.B)
        // test four corners of full resolution with small values
        val cornersWithData = Seq(
          (0.U, 0.U, 11.U),
          (319.U, 0.U, 22.U),
          (0.U, 279.U, 33.U),
          (319.U, 279.U, 44.U)
        )
        for ((x, y, data) <- cornersWithData) {
          dut.io.x.poke(x)
          dut.io.y.poke(y)
          dut.io.dataIn.poke(data)
          dut.clock.step()
          dut.io.dataOut.expect(data)

          dut.io.write.poke(false.B)
          dut.clock.step()
          dut.io.dataOut.expect(data)
          dut.io.write.poke(true.B)
        }
      }
    }

  }
}
