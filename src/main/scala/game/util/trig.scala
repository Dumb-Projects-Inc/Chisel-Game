package gameEngine.trig

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

object TrigLUT {
  val samples = 256
  val width = 32
  val frac = 16

  private val samplesU = samples.U
  private val mirrorIdxYU = (samples - 1).U
}

class TrigLUT extends Module {
  val io = IO(new Bundle {
    val angle = Input(SInt(TrigLUT.width.W)) // Q16.16 input
    val sin, cos, tan, sec, csc, cot = Output(SInt(TrigLUT.width.W))
  })

  import TrigLUT._
  val sinTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * i.toDouble / samples
    toFP(math.sin(theta))
  }

  // Quarter-cycle secant table [0..π/2)
  val secTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * i.toDouble / samples
    toFP(1.0 / math.cos(theta))
  }

  val cscTable = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * (i.toDouble + 0.5) / samples
    toFP(1.0 / math.sin(theta))
  }

  // constant 2/pi in Q16.16
  val twoOverPi = toFP(2.0 / math.Pi)

  // scale angle into [0, 4) in Q16.16
  val scaled = io.angle.fpMul(twoOverPi)

  // extract quadrant (integer part mod 4) and fractional part
  val quadrant = (scaled >> frac).asUInt & 3.U
  val fracPart = scaled(frac - 1, 0).asUInt

  // compute base index into quarter-cycle tables
  val idxExt = (fracPart * samplesU) >> frac
  val idx = idxExt(log2Ceil(samples) - 1, 0)

  io.sin := MuxLookup(quadrant, 0.S)(
    Seq(
      0.U -> sinTable(idx),
      1.U -> sinTable(mirrorIdxYU - idx),
      2.U -> -sinTable(idx),
      3.U -> -sinTable(mirrorIdxYU - idx)
    )
  )

  // cosine shift quadrant
  io.cos := MuxLookup((quadrant + 1.U) & 3.U, 0.S)(
    Seq(
      0.U -> sinTable(idx),
      1.U -> sinTable(mirrorIdxYU - idx),
      2.U -> -sinTable(idx),
      3.U -> -sinTable(mirrorIdxYU - idx)
    )
  )

  io.sec := MuxLookup(quadrant, 0.S)(
    Seq(
      0.U -> secTable(idx),
      1.U -> -secTable(mirrorIdxYU - idx),
      2.U -> -secTable(idx),
      3.U -> secTable(mirrorIdxYU - idx)
    )
  )

  io.csc := MuxLookup(quadrant, 0.S)(
    Seq(
      0.U -> cscTable(idx),
      1.U -> cscTable(mirrorIdxYU - idx),
      2.U -> -cscTable(idx),
      3.U -> -cscTable(mirrorIdxYU - idx)
    )
  )

  io.tan := io.sin.fpMul(io.sec)
  io.cot := io.cos.fpMul(io.csc)
}
