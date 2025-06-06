package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class WallEntity(
    width: Int,
    color: UInt
) extends Module {
  val io = IO(new Bundle {
    val screen = new Bundle {
      val x = Input(UInt(width.W))
      val y = Input(UInt(width.W))
    }
    val visible = Output(Bool())
    val pixel = Output(UInt(12.W))
    val p1, p2, p3, p4 = Input(new Vec2(UInt(width.W)))
  })

  val px = io.screen.x
  val py = io.screen.y

  def cross(a: Vec2[UInt], b: Vec2[UInt]): SInt = {
    ((b.x.asSInt - a.x.asSInt) * (py.asSInt - a.y.asSInt)) -
      ((b.y.asSInt - a.y.asSInt) * (px.asSInt - a.x.asSInt))
  }

  val c1 = cross(io.p1, io.p2)
  val c2 = cross(io.p2, io.p3)
  val c3 = cross(io.p3, io.p4)
  val c4 = cross(io.p4, io.p1)

  val allPos = (c1 >= 0.S) && (c2 >= 0.S) && (c3 >= 0.S) && (c4 >= 0.S)
  val allNeg = (c1 <= 0.S) && (c2 <= 0.S) && (c3 <= 0.S) && (c4 <= 0.S)

  io.visible := allPos || allNeg
  io.pixel := Mux(io.visible, color, 0.U)
}
