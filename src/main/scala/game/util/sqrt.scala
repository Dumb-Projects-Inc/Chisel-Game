package gameEngine.fixed

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

class InverseSqrt(errorMargin: Double = 0.1) extends Module {
  val io = IO(new Bundle {
    val input = Input(SInt(32.W))
    val wrEn = Input(Bool())
    val output = Output(SInt(32.W))
    val ready = Output(Bool())
  })
  val lutSize = 256
  val lut = VecInit.tabulate(lutSize) { i =>
    val real = 1.0 / math.sqrt(1.0 + i.toDouble / lutSize)
    toFP(real)
  }

  // Y_(n+1) = y_n * (3 - x*y_n^2)/2

  val xReg = Reg(SInt(32.W))
  val yReg = Reg(SInt(32.W))
  val readyReg = RegInit(false.B)

  when(io.wrEn) {
    xReg := io.input
    val xU = io.input.asUInt
    val leading = PriorityEncoder(Reverse(xU(31, 16))) // count leading zeros
    val shiftAmt = leading
    val normalized = (xU << shiftAmt)(31, 0).asSInt // shift to [1.0, 2.0)

    val lutIndex = (normalized.asUInt >> (32 - log2Ceil(lutSize))).asUInt
    val guess = lut(lutIndex)
    val outShift = shiftAmt >> 1
    val scaledGuess = guess >> outShift
    yReg := scaledGuess
    readyReg := false.B
  }

  // n+1 iteration
  val y2 = yReg.fpMul(yReg)
  val half_x = xReg.fpMul(toFP(0.5)) // TODO: replace with a shift
  val term = toFP(1.5) - half_x.fpMul(y2)
  val yNext = yReg.fpMul(term)

  // |1 - x * y^2|
  val err = (toFP(1.0) - xReg.fpMul(y2)).fpAbs()
  val done = err < toFP(errorMargin)

  // Update guess
  when(!readyReg && !io.wrEn) {
    yReg := yNext
    readyReg := done
  }

  io.output := yReg.fpAbs()
  io.ready := readyReg

}
