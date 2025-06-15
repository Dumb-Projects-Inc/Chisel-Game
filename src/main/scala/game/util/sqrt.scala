package gameEngine.fixed

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

object InverseSqrt {
  object State extends ChiselEnum {
    val idle = Value
  }
}

/* This module implements an approximator for the inverse squareroot.
 *
 * It is approximated by newtons method down to a iterative algo:
 * y_(n+1) = y_n * (3 - x*y_n^2)/2
 * Here x is the input, y_n is the current guess
 * */
class InverseSqrt(iterations: Int = 4) extends Module {
  val io = IO(new Bundle {
    val input = Flipped(Decoupled(SInt(24.W)))
    val result = Decoupled(SInt(24.W))
  })
  // LUT for initial guess, based on most significant bit
  val lutVec = VecInit.tabulate(24) { i =>
    toFP(1.0 / math.sqrt(math.pow(2, 12 - i)))
  }

  val x = RegInit(0.S(24.W))
  val y = RegInit(0.S(24.W))
  val i = RegInit(0.U(log2Ceil(iterations).W))

  object S extends ChiselEnum {
    val idle, compute, done = Value
  }
  val state = RegInit(S.idle)

  io.input.ready := false.B
  io.result.valid := false.B
  io.result.bits := DontCare
  switch(state) {
    is(S.idle) {
      io.input.ready := true.B
      when(io.input.valid) {
        x := io.input.bits
        val idx = PriorityEncoder(Reverse(io.input.bits.asUInt))
        y := lutVec(idx)
        i := 0.U
        state := S.compute
      }
    }
    is(S.compute) {
      val threeHalfs = toFP(1.5)
      val halfX = x >> 1.U
      y := y.fpMul(threeHalfs - halfX.fpMul(y.fpMul(y)))
      i := i + 1.U
      when(i === (iterations - 1).U) {
        state := S.done
      }
    }
    is(S.done) {
      io.result.bits := y
      io.result.valid := true.B
      when(io.result.ready) {
        state := S.idle
      }
    }
  }
}
