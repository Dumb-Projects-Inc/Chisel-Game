package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class NewWallEntity(numColors: Int, color: Int) extends Module {
     val io = IO(new Bundle {
    val x, y   = Input(UInt(9.W))
    val p1, p2 = Input(new Vec2(UInt(9.W)))
    val visible = Output(Bool())
    val colorOut = Output(UInt(log2Ceil(numColors).W))
  })

  val p1xLower = io.p1.x < io.p2.x
  val xMin      = Mux(p1xLower, io.p1.x, io.p2.x)
  val xMax      = Mux(p1xLower, io.p2.x, io.p1.x)
  val dx        = xMax - xMin

  val inXRange = io.x >= xMin && io.x <= xMax
  val segments = 32
  val sbits    = log2Ceil(segments)
  val thresholds = VecInit(
    (1 until segments).map(i => xMin + ((dx * i.U) >> sbits))
  )
  val segIdx = PriorityMux(
    (0 until segments-1).map(i => (io.x < thresholds(i)) -> i.U(sbits.W)) :+
    (true.B -> (segments-1).U)
  )

  val yStartS  = Mux(p1xLower, io.p1.y, io.p2.y).asSInt
  val yEndS    = Mux(p1xLower, io.p2.y, io.p1.y).asSInt
  val yDeltaS  = yEndS - yStartS

  val yStepsS  = Wire(Vec(segments, SInt(10.W)))
  for (i <- 0 until segments) {
    yStepsS(i) := yStartS + ((yDeltaS * i.S) >> sbits)
  }

  val topY   = yStepsS(segIdx).asUInt
  val botY   = 239.U - topY
  val vis    = inXRange && io.y >= topY && io.y <= botY

  io.visible  := vis
  io.colorOut := Mux(vis, color.U, 0.U)
}

class FinalWallEntity(numColors: Int, color: Int, numPoints: Int) extends Module {
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
