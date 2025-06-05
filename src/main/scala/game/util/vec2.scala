package gameEngine.vec2

import chisel3._
import gameEngine.fixed.FixedPointUtils._

object Vec2 {
  def apply(x: SInt, y: SInt): Vec2 = {
    val v = Wire(new Vec2())
    v.x := x; v.y := y
    v
  }
}

class Vec2 extends Bundle {
  val x, y = SInt(32.W)

  def +(that: Vec2): Vec2 = {
    Vec2(this.x + that.x, this.y + that.y)
  }

  def dist2(that: Vec2) =
    (
      (this.x - that.x).fpMul(this.x - that.x)
        +& (this.y - that.y).fpMul(this.y - that.y)
    )
}
