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
import gameEngine.entity.library.FinalWallEntity
import gameEngine.entity.library.SmileyEntity

class WallBandDemo extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
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

  val buf = Module(new DualPaletteFrameBuffer(doomPalette))
  io.vga := buf.io.vga

  val bob = Module(new SmileyEntity(8, doomPalette))
  bob.io.scale := 50.S
  bob.io.screen.x := DontCare
  bob.io.screen.y := DontCare
  bob.io.setPos.wrEn := false.B
  bob.io.setPos.x := 0.U
  bob.io.setPos.y := 0.U

  val points = RegInit(
    VecInit(
      (0 until 239).map(_.U)
    )
  )

  val wall = Module(new FinalWallEntity(doomPalette.length, 8, points.length))
  wall.io.points := points
  wall.io.x := DontCare
  wall.io.y := DontCare

  val filling :: waiting :: Nil = Enum(2)
  val state = RegInit(filling)
  val (x, xWrap) = Counter(state === filling, 320)
  val (y, yWrap) = Counter(xWrap && (state === filling), 240)
  
  val (gameCount, gameTick) = Counter(true.B, 2000000)
  when(gameTick) {
    points := VecInit(points.map(_ + 1.U))
  }

  buf.io.valid := false.B
  buf.io.wEnable := false.B
  buf.io.x := 0.U
  buf.io.y := 0.U
  buf.io.dataIn := 0.U

  switch(state) {
    is(filling) {
      buf.io.x := x
      buf.io.y := y
      buf.io.wEnable := true.B

      wall.io.x := x
      wall.io.y := y

      bob.io.screen.x := x
      bob.io.screen.y := y
      bob.io.setPos.wrEn := true.B
      bob.io.setPos.x := 0.U
      bob.io.setPos.y := 0.U

      buf.io.dataIn := PriorityMux(
        Seq(
          bob.io.visible -> bob.io.pixel,
          wall.io.visible -> wall.io.colorOut,
          true.B -> 0.U
        )
      )

      when(xWrap && yWrap) { state := waiting }
    }
    is(waiting) {
      buf.io.valid := true.B
      when(buf.io.newFrame) {
        state := filling
      }
    }
  }
}
object WallBandDemoMain {
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
        ChiselGeneratorAnnotation(() => new WallBandDemo),
        FirtoolOption("--disable-all-randomization")
      )
    )
  }
}
