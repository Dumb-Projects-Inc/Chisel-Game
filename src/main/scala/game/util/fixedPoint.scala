package gameEngine.fixed

import chisel3._
import chisel3.util._

object FixedPointUtils {
  val width = 32
  val frac = 16

  def toFP(x: Double): SInt = {
    val raw = BigInt((x * (1 << frac)).round)
    raw.S(width.W)
  }

  def fromFP(x: SInt): Double =
    x.litValue.toDouble / (1 << frac)

  implicit class RichFP(val self: SInt) {

    def fpMul(that: SInt): SInt = {
      require(
        self.getWidth == width,
        s"fpMul: left operand width ${self.getWidth} != expected $width"
      )
      require(
        that.getWidth == width,
        s"fpMul: right operand width ${that.getWidth} != expected $width"
      )
      val prod = ((self * that) >> frac)(width - 1, 0).asSInt
      require(
        prod.getWidth == width
      )
      prod

    }

    def fpFloor: SInt = {
      require(
        self.getWidth == width,
        s"fpFloor: operand width ${self.getWidth} != expected $width"
      )
      val mask = (~((BigInt(1) << frac) - 1)).S(width.W)
      self & mask

    }

    def fpCeil: SInt = {
      require(
        self.getWidth == width,
        s"fpCeil: operand width ${self.getWidth} != expected $width"
      )
      Mux(self.fpFrac === toFP(0.0), self, self.fpFloor + toFP(1.0))
    }

    def fpFrac: SInt = {
      require(
        self.getWidth == width,
        s"fpFrac: operand width ${self.getWidth} != expected $width"
      )
      val mask = ((BigInt(1) << frac) - 1).S(width.W)
      val absVal = Mux(self >= 0.S, self, -self)
      absVal & mask
    }

    def toDouble: Double =
      self.litValue.toDouble / (1 << frac)
  }
}
