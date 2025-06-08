package gameEngine

import chisel3._
import chisel3.util.{log2Ceil, Counter}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer

// TODO:
// Generer background igennem tilemap
// - Gem en af hvert tile
// - gem 2d array som repræsenterer verdenen. Hver index holder en ref til et tile
// - skal være scrollable
//
//
// Sprite/Entity interface
// - Entities repræsenterer en ting i spillet. Indeholder:
//    - interface som gør den renderbar for scene
//    - interface så spil logik kan interagere med den
//
//
class Scene2D(numColors: Int) extends Module {
  val io = IO(new Bundle {
    // Control signal
    val done = Output(Bool())
    val next = Input(Bool())

    // Signals used to write to buffer
    val x = Output(UInt(log2Ceil(320).W))
    val y = Output(UInt(log2Ceil(240).W))
    val write = Output(Bool())
    val paletteOut = Output(UInt(log2Ceil(numColors).W))
  })

}

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
