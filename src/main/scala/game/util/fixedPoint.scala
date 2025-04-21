package gameEngine.fixed

import chisel3._
import chisel3.util._
import chisel3.SIntFactory

object FixedPointUtils {
  def toFixed(value: Double, width: Int, frac: Int): SInt = {
    val raw = BigInt(math.round(value * (1 << frac)))
    raw.S(width.W)
  }

  def mul(a: SInt, b: SInt, width: Int, frac: Int): SInt = {
    val prod = a * b
    val shifted = prod >> frac
    shifted.asTypeOf(SInt(width.W))
  }
}
