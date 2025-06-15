package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class WallEntity(numColors: Int, colorIdx: Int, numPoints: Int) extends Module {
  private val width = 320
  private val height = 240
  val colorW = log2Ceil(numColors)

  val io = IO(new Bundle {
    val x = Input(UInt(log2Ceil(width).W))
    val y = Input(UInt(log2Ceil(height).W))
    val heights = Input(Vec(numPoints, UInt(log2Ceil(height).W)))
    val visible = Output(Bool())
    val color = Output(UInt(colorW.W))
  })

  val segmentWidth = (width / numPoints).U
  val segmentIdx = io.x / segmentWidth

  val h = io.heights(segmentIdx)

  val top = 120.U - (h / 2.U)
  val bot = 120.U + (h / 2.U)

  val vis = (io.y >= top) && (io.y < bot)
  io.visible := vis
  io.color := Mux(vis, colorIdx.U, DontCare)
}
