package gameEngine.trig

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

object TrigLUT {
  val samples = 256
  val width = 24
  val frac = 12

  private val samplesU = samples.U
  private val mirrorIdxYU = (samples - 1).U
  val maxFP = (BigInt(1) << (width - 1)) - 1
}

class TrigLUT extends Module {
  val io = IO(new Bundle {
    val angle = Input(SInt(TrigLUT.width.W)) // Q12.12 input
    val sin, cos = Output(SInt(TrigLUT.width.W))
    val tan, sec = Output(SInt(TrigLUT.width.W))
    val csc, cot = Output(SInt(TrigLUT.width.W))
  })

  import TrigLUT._

  val sinTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * i.toDouble / samples
    toFP(math.sin(theta))
  }
  val secTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * i.toDouble / samples
    toFP(1.0 / math.cos(theta))
  }
  val cscTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * (i.toDouble + 0.5) / samples
    toFP(1.0 / math.sin(theta))
  }

  val tanTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * i.toDouble / samples
    toFP(math.tan(theta))
  }

  val cotTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val theta = math.Pi / 2 * i.toDouble / samples
    val raw = 1.0 / math.tan(theta)
    val clipped = if (raw.isInfinite) maxFP.toDouble / (1 << frac) else raw
    toFP(clipped)
  }

  val twoOverPi = toFP(2.0 / math.Pi)
  val scaled = io.angle.fpMul(twoOverPi)
  val quadrant = (scaled >> frac).asUInt & 3.U
  val fracPart = scaled(frac - 1, 0).asUInt
  val idxExt = (fracPart * samplesU) >> frac
  val idx = RegNext(idxExt(log2Ceil(samples) - 1, 0))

  io.sin := MuxLookup(quadrant, 0.S)(
    Seq(
      0.U -> sinTable(idx),
      1.U -> sinTable(mirrorIdxYU - idx),
      2.U -> -sinTable(idx),
      3.U -> -sinTable(mirrorIdxYU - idx)
    )
  )
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

  io.tan := MuxLookup(quadrant, 0.S)(
    Seq(
      0.U -> tanTable(idx),
      1.U -> -tanTable(mirrorIdxYU - idx),
      2.U -> tanTable(idx),
      3.U -> -tanTable(mirrorIdxYU - idx)
    )
  )
  io.cot := MuxLookup(quadrant, 0.S)(
    Seq(
      0.U -> cotTable(idx),
      1.U -> -cotTable(mirrorIdxYU - idx),
      2.U -> cotTable(idx),
      3.U -> -cotTable(mirrorIdxYU - idx)
    )
  )
}
