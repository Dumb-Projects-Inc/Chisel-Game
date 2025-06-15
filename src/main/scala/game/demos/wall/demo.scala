package gameEngine.demos

import chisel3._
import chisel3.util.{log2Ceil, Counter, switch, is, Enum, PriorityMux}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.entity.library.WallEntity
import gameEngine.vec2.Vec2._
import gameEngine.vec2.Vec2
import gameEngine.entity.library.SquareEntity
import gameEngine.entity.library.NewWallEntity

class MovingWallDemo extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val moveForward = Input(Bool()) // walk forward
    val moveBackward = Input(Bool()) // walk backward
    val lookLeft = Input(Bool())
    val lookRight = Input(Bool())
  })

  val doomPalette = Seq(
    "h000".U(12.W), // #000000
    "h222".U(12.W), // #222222
    "h444".U(12.W), // #444444
    "h777".U(12.W), // #777777
    "hAAA".U(12.W), // #AAAAAA
    "hFFF".U(12.W), // #FFFFFF

    "h430".U(12.W), // #443300
    "h640".U(12.W), // #664400
    "hA50".U(12.W), // #AA5500
    "hD90".U(12.W), // #DD9900

    "hF20".U(12.W), // #FF2200
    "hF60".U(12.W), // #FF6600
    "hFF0".U(12.W), // #FFFF00

    "hA00".U(12.W), // #AA0000
    "hF00".U(12.W), // #FF0000
    "h800".U(12.W) // #880000
  )

  val (gameCount, gameTick) = Counter(true.B, 2000000)

  val buf = Module(new DualPaletteFrameBuffer(doomPalette))
  io.vga := buf.io.vga

  // "Distance" simulation: 0 (far) to 100 (close)
  val dist = RegInit(50.U(8.W))
  val look = RegInit(50.U(8.W))
  when(gameTick) {
    when(io.moveForward && dist < 100.U) {
      dist := dist + 1.U
    }
    when(io.moveBackward && dist > 10.U) {
      dist := dist - 1.U
    }
    when(io.lookRight && look < 100.U) {
      look := look + 1.U
    }
    when(io.lookLeft && look > 10.U) {
      look := look - 1.U
    }
  }

  val wallWidth = Wire(UInt(9.W))
  val wallHeight = Wire(UInt(9.W))
  wallWidth := 20.U + (dist >> 1) 
  wallHeight := 40.U + dist 

  val centerX = 160.U
  val centerY = 120.U

  // BACK WALL (yellow)
  val top = 120.U - (wallHeight >> 1)
  val bottom = 120.U + (wallHeight >> 1)
  val backWall = Module(new SquareEntity(doomPalette.length, 12))
  backWall.io.p1 := Vec2(110.U - wallWidth + look, top)
  backWall.io.p2 := Vec2(110.U + wallWidth + look, bottom)
  backWall.io.p := DontCare

  // Ground
  val ground = Module(new SquareEntity(doomPalette.length, 4))
  ground.io.p1 := Vec2(0.U, 120.U)
  ground.io.p2 := Vec2(320.U, 120.U)
  ground.io.p := DontCare

  // LEFT WALL
  val leftWall = Module(new NewWallEntity(doomPalette.length, 14))
  leftWall.io.x := DontCare
  leftWall.io.y := DontCare
  leftWall.io.p1 := Vec2(0.U, 0.U)
  leftWall.io.p2 := Vec2(110.U - wallWidth + look, top)

  // RIGHT WALL
  val rightWall = Module(new NewWallEntity(doomPalette.length, 14))
  rightWall.io.x := DontCare
  rightWall.io.y := DontCare
  rightWall.io.p1 := Vec2(110.U + wallWidth + look, top)
  rightWall.io.p2 := Vec2(319.U, 0.U)

  // Drawing loop
  val filling :: waiting :: Nil = Enum(2)
  val state = RegInit(filling)

  val (x, xWrap) = Counter(state === filling, 320)
  val (y, yWrap) = Counter(xWrap, 240)

  buf.io.valid := false.B
  buf.io.wEnable := false.B
  buf.io.x := 0.U
  buf.io.y := 0.U
  buf.io.dataIn := 0.U

  switch(state) {
    is(filling) {
      buf.io.wEnable := true.B
      buf.io.x := x
      buf.io.y := y

      /* backWall.io.x := x
      backWall.io.y := y */
      backWall.io.p := Vec2(x, y)

      ground.io.p := Vec2(x, y)

      leftWall.io.x := x
      leftWall.io.y := y

      rightWall.io.x := x
      rightWall.io.y := y

      buf.io.dataIn := PriorityMux(
        Seq(
          leftWall.io.visible -> leftWall.io.colorOut,
          rightWall.io.visible -> rightWall.io.colorOut,
          backWall.io.visible -> backWall.io.colorOut,
          ground.io.visible -> ground.io.colorOut,
          true.B -> 0.U
        )
      )

      when(xWrap && yWrap) {
        state := waiting
      }
    }

    is(waiting) {
      buf.io.valid := true.B
      when(buf.io.newFrame) {
        state := filling
      }
    }
  }
}

object MovingWallDemoMain {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).execute(
      Array(
        "--target",
        "systemverilog",
        "--target-dir",
        "generated",
        "--split-verilog"
      ),
      Seq(
        ChiselGeneratorAnnotation(() => new MovingWallDemo),
        FirtoolOption("--disable-all-randomization")
      )
    )
  }
}
