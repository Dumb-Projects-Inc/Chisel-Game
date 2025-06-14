package gameEngine.fixed

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

object InverseSqrt {
  object State extends ChiselEnum {
    val sReady = Value
  }
}

class InverseSqrt(errorMargin: Double = 0.1) extends Module {
  import InverseSqrt.State
  import InverseSqrt.State._
  val io = IO(new Bundle {
    val input = Input(SInt(24.W))
    val wrEn = Input(Bool())
    val output = Output(SInt(24.W))
    val ready = Output(Bool())
  })
  val lutSize = 256
  val lut = VecInit.tabulate(lutSize) { i =>
    // assume input is in ]0.0, 100.0]
    val maxVal = 100.0
    val step = maxVal / lutSize
    val real = (1.0 / math.sqrt((i + 1) * step))
    toFP(real)
  }

  // Y_(n+1) = y_n * (3 - x*y_n^2)/2
  // where x is the input and y_n is the current approximation

  val x = RegInit(0.S(24.W))
  val y = RegInit(0.S(24.W))
  val ready = RegInit(false.B)

  val state = RegInit(sReady)

  // state registers
  val ySquared = RegInit(0.S(24.W))
  val xy2 = RegInit(0.S(24.W))
  val half = RegInit(0.S(24.W))

  switch(state) {
    is(sReady) {
      when(io.wrEn) {
        x := io.input
        // Initialize y using LUT
        val maxInputQ1212 = (100.0 * (1 << 12)).toInt.U // 409600

        val inputClamped =
          Mux(io.input <= 0.S, 1.S, io.input) // Clamp below zero
        val rawIndex = (inputClamped.asUInt * (lutSize - 1).U) / maxInputQ1212
        val index = Mux(rawIndex >= lutSize.U, (lutSize - 1).U, rawIndex)
        y := lut(index)
        ready := true.B
        state := sReady
      }
    }

  }

  io.ready := ready
  io.output := y
}
