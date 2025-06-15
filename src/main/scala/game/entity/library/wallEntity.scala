package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class WallEntity(numColors: Int, color: Int, numPoints: Int) extends Module {
  val io = IO(new Bundle {
    val x = Input(UInt(9.W))
    val y = Input(UInt(9.W))
    val points = Input(Vec(numPoints, UInt(9.W)))
    val visible = Output(Bool())
    val colorOut = Output(UInt(log2Ceil(numColors).W))
  })

  val segmentBits = log2Ceil(numPoints)
  val segmentWidth = (320 / numPoints).U
  val segmentIdx = io.x / segmentWidth

  val clampedIdx = RegNext(Mux(segmentIdx >= numPoints.U, (numPoints - 1).U, segmentIdx))
  val height = RegNext(io.points(clampedIdx))

  val topY = 120.U - (height >> 1)
  val botY = 120.U + (height >> 1)

  val visible = io.y >= topY && io.y < botY

  io.visible := RegNext(visible)
  io.colorOut := RegNext(Mux(visible, color.U, 0.U))
}
