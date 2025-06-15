package gameEngine.fixed

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

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
    val idle, compute0, compute1, compute2, done = Value
  }
  val state = RegInit(S.idle)

  // Intermediate registers
  val y2 = RegInit(0.S(24.W))
  val halfTimesy2 = RegInit(0.S(24.W))

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
        state := S.compute0
      }
    }
    is(S.compute0) {
      y2 := y.fpMul(y)
      state := S.compute1
    }
    is(S.compute1) {
      val halfX = x >> 1.U
      halfTimesy2 := halfX.fpMul(y2)
      state := S.compute2
    }
    is(S.compute2) {
      val threeHalfs = toFP(1.5)
      y := y.fpMul(threeHalfs - halfTimesy2)
      i := i + 1.U
      when(i === (iterations - 1).U) {
        state := S.done
      }.otherwise {
        state := S.compute0
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
