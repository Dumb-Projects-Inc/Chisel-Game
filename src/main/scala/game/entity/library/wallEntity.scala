package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class WallEntity(numColors: Int, color: Int) extends Module {
  val io = IO(new Bundle {
    val x, y = Input(UInt(9.W)) // Ensure wide enough for 320x240
    val p1, p2 =
      Input(new Vec2(UInt(9.W))) // x defines x position, y defines height
    val visible = Output(Bool())
    val colorOut = Output(UInt(log2Ceil(numColors).W))
  })

  val px = io.x
  val py = io.y

  // We know the two lines are vertical, so we can use a simple check
  val gap1 = (240.U - io.p1.y) >> 1
  val gap2 = (240.U - io.p2.y) >> 1

  // Largest gap has the smallest line height
  val largestGap = Mux(io.p1.y < io.p2.y, gap1, gap2)
  val smallestGap = Mux(io.p1.y > io.p2.y, gap1, gap2)

  val visible = Wire(Bool())
  visible := false.B // Default to not visible
  when((px >= io.p1.x) && (px <= io.p2.x)) {
    // Check as a rectangle first
    val rectangle = (py >= largestGap) && (py <= 240.U - largestGap)
    visible := rectangle
    when(!rectangle) {
      //val xMin = Mux(io.p1.x < io.p2.x, io.p1.x, io.p2.x)
      //val xMax = Mux(io.p1.x > io.p2.x, io.p1.x, io.p2.x)
      //val dx = xMax - xMin
//
      //// Precompute thresholds that split the wall horizontally into 8 equal segments
      //val thresholds = VecInit((1 until 8).map(i => xMin + ((dx * i.U) >> 3)))
//
      //val segmentIdx = Wire(UInt(3.W))
//
      //segmentIdx := PriorityMux(
      //  (0 until 7).map(i => (px < thresholds(i)) -> i.U(3.W)) :+
      //    (true.B -> 7.U)
      //)
//
      //val gapStep = (largestGap - smallestGap) >> 3
//
      //val currentTopY = smallestGap + (gapStep * segmentIdx)
      //val currentBotY = (240.U - smallestGap) - (gapStep * segmentIdx)
//
      //when(py < largestGap) {
      //  visible := py >= currentTopY
      //}
      //when(py > (240.U - largestGap)) {
      //  visible := py <= currentBotY
      //}
    }
  }
  // only calculate triangle if not

  io.visible := visible
  io.colorOut := Mux(
    visible,
    color.U,
    0.U(log2Ceil(numColors).W)
  ) // Output color if visible, else 0
}


class SquareEntity(numColors: Int, color: Int) extends Module {
  val screenWidth = 320
  val screenHeight = 240

  val width = log2Ceil(screenWidth.max(screenHeight))

  val io = IO(new Bundle {
    val p = Input(new Vec2(UInt(width.W)))
    val p1 = Input(new Vec2(UInt(width.W)))
    val p2 = Input(new Vec2(UInt(width.W)))
    val visible = Output(Bool())
    val colorOut = Output(UInt(log2Ceil(numColors).W))
  })

  val xMin = Mux(io.p1.x < io.p2.x, io.p1.x, io.p2.x)
  val xMax = Mux(io.p1.x < io.p2.x, io.p2.x, io.p1.x)
  val yMin = Mux(io.p1.y < io.p2.y, io.p1.y, io.p2.y)
  val yMax = Mux(io.p1.y < io.p2.y, io.p2.y, io.p1.y)

  val insideX = io.p.x >= xMin && io.p.x < xMax
  val insideY = io.p.y >= yMin && io.p.y < yMax

  io.visible := insideX && insideY

  private val wallColor = color.U(log2Ceil(numColors).W)
  io.colorOut := Mux(io.visible, wallColor, 0.U)
}
