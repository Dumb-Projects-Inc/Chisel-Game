package gameEngine

import chisel3._
import chisel3.util.{log2Ceil, Counter, switch, is, Enum, PriorityMux}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer

class Engine extends Module {
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

  val wall1 = Module(new WallEntity(doomPalette.length, 9))

  wall1.io.p1 := Vec2(0.U, 0.U)
  wall1.io.p2 := Vec2(0.U, 240.U)
  wall1.io.p3 := Vec2(100.U, 200.U)
  wall1.io.p4 := Vec2(100.U, 40.U)

  wall1.io.x := DontCare
  wall1.io.y := DontCare

  val wall2 = Module(new WallEntity(doomPalette.length, 9))

  wall2.io.p1 := Vec2(320.U, 0.U)
  wall2.io.p2 := Vec2(320.U, 240.U)
  wall2.io.p3 := Vec2(220.U, 200.U)
  wall2.io.p4 := Vec2(220.U, 40.U)

  wall2.io.x := DontCare
  wall2.io.y := DontCare

  // Connect screen to buffer
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

      wall1.io.x := x
      wall1.io.y := y
      wall2.io.x := x
      wall2.io.y := y

      buf.io.dataIn := Mux(
        wall1.io.visible,
        wall1.io.colorOut,
        Mux(wall2.io.visible, wall2.io.colorOut, 0.U)
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

  io.vga := buf.io.vga

}
object gameEngineMain {
  def main(args: Array[String]): Unit = {
    // Splitting files is expected, and the blackbox will generate syntax errors if not split
    (new ChiselStage).execute(
      Array(
        "--target",
        "systemverilog",
        "--target-dir",
        "generated",
        "--split-verilog"
      ),
      Seq(
        ChiselGeneratorAnnotation(() => new Engine),
        FirtoolOption("--disable-all-randomization")
      )
    )

  }
}
