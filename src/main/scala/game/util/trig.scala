package gameEngine.fixed

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._

object TrigLUT {
  val samples = 256
  val width = 32
  val frac = 16

  val sinTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val angle = math.Pi / 2 * i.toDouble / samples.toDouble
    toFixed(math.sin(angle), width, frac)
  }

  val secTable: Vec[SInt] = VecInit.tabulate(samples) { i =>
    val angle = math.Pi / 2 * i.toDouble / samples.toDouble
    toFixed(1.0 / math.cos(angle), width, frac)
  }
}
