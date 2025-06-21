package gameEngine.vec2

import chisel3._
import gameEngine.fixed.FixedPointUtils._

class Vec2[T <: Data](gen: T) extends Bundle {
  val x, y = gen.cloneType

}

object Vec2 {
  def apply[T <: Data](x: T, y: T): Vec2[T] = {
    val w = Wire(new Vec2(x.cloneType))
    w.x := x
    w.y := y
    w
  }

  implicit class Vec2SIntOps(val a: Vec2[SInt]) {
    def +(b: Vec2[SInt]): Vec2[SInt] = {
      Vec2(a.x + b.x, a.y + b.y)
    }

    def -(b: Vec2[SInt]): Vec2[SInt] = {
      Vec2(a.x - b.x, a.y - b.y)
    }

    def dist2Fp(b: Vec2[SInt]): SInt = {
      val dX = a.x - b.x
      val dY = a.y - b.y
      dX.fpMul(dX) +& dY.fpMul(dY)

    }

    def distFpApprox(b: Vec2[SInt]): SInt = {
      val absX = (a.x - b.x).abs
      val absY = (a.y - b.y).abs
      val mx = Mux(absX > absY, absX, absY)
      val mn = Mux(absX > absY, absY, absX)
      val approx = mx + (mn >> 1)
      approx
    }

  }

  implicit class Vec2UIntOps(val a: Vec2[UInt]) {
    def +(b: Vec2[UInt]): Vec2[UInt] = {
      Vec2(a.x + b.x, a.y + b.y)
    }
  }

}
