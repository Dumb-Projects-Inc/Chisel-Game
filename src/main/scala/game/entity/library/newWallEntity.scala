package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class NewWallEntity(numColors: Int, color: Int) extends Module {
   val io = IO(new Bundle {
    val x, y = Input(UInt(9.W))
    val p1, p2 = Input(new Vec2(UInt(9.W)))
    val visible = Output(Bool())
    val colorOut = Output(UInt(log2Ceil(numColors).W))
  })

  val px = io.x
  val py = io.y

  val xMin = Mux(io.p1.x < io.p2.x, io.p1.x, io.p2.x)
  val xMax = Mux(io.p1.x > io.p2.x, io.p1.x, io.p2.x)
  val dx = xMax - xMin

  // Keeping track if we are inside the x range that the wall defines
  val inXRange = px >= xMin && px <= xMax

  // Segmenting wall (8 segments)
  val numSegments = 8
  val segmentBits = log2Ceil(numSegments)

  // Mapping the segments of the wall into thresholds (thresholds(0) = xMin + 1/8 * dx ... thresholds(1) = xMin + 2/8 * dx) and so on...
  val thresholds = VecInit((1 until numSegments).map(i => xMin + ((dx * i.U) >> segmentBits)))

  // Simply checks what segment px is in by mapping through a priroty mux with all of the segments mapped out.
  val segmentIdx = Wire(UInt(segmentBits.W))
  segmentIdx := PriorityMux(
    (0 until numSegments - 1).map(i => (px < thresholds(i)) -> i.U(segmentBits.W)) :+
      (true.B -> (numSegments - 1).U)
  )

  val ySteps = Wire(Vec(numSegments, UInt(9.W)))

  val yStart = Mux(io.p1.x < io.p2.x, io.p1.y, io.p2.y)
  val yEnd = Mux(io.p1.x < io.p2.x, io.p2.y, io.p1.y)

  val yDelta = yEnd - yStart
  for (i <- 0 until numSegments) {
    ySteps(i) := yStart + ((yDelta * i.U) >> segmentBits)
  }

  val topY = ySteps(segmentIdx)
  val bottomY = 239.U - topY // Symmetric for now; can expand later

  val visible = inXRange && py >= topY && py <= bottomY

  io.visible := visible
  io.colorOut := Mux(visible, color.U, 0.U)
}