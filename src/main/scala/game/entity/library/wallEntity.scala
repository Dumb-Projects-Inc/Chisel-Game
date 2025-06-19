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

class WallSegment(maxHeight: Int) extends Bundle {
  val height = UInt(log2Ceil(maxHeight).W)
  val isHorizontal = Bool()
}
object WallSegment {
  def apply(maxHeight: Int): WallSegment = {
    val w = Wire(new WallSegment(maxHeight))
    w.height := 0.U(log2Ceil(maxHeight).W)
    w.isHorizontal := false.B
    w
  }
}

class ShadedWallEntity(
    numColors: Int,
    colorIdx: Int,
    shadedColorIdx: Int,
    numPoints: Int
) extends Module {
  private val width = 320
  private val height = 240
  val colorW = 4 // Assuming a fixed color width for simplicity

  val io = IO(new Bundle {
    val x = Input(UInt(log2Ceil(width).W))
    val y = Input(UInt(log2Ceil(height).W))
    val segments = Input(Vec(numPoints, new WallSegment(height)))

    val visible = Output(Bool())
    val color = Output(UInt(colorW.W))
  })

  val segmentWidth = (width / numPoints).U
  val segmentIdx = io.x / segmentWidth

  val h = io.segments(segmentIdx).height
  val isHorizontal = io.segments(segmentIdx).isHorizontal

  val top = 120.U - (h / 2.U)
  val bot = 120.U + (h / 2.U)

  val vis = (io.y >= top) && (io.y < bot)
  io.visible := vis
  io.color := Mux(
    vis,
    Mux(isHorizontal, colorIdx.U, shadedColorIdx.U),
    DontCare
  )
}
