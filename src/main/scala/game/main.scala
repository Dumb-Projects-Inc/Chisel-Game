package gameEngine

import chisel3._
import chisel3.util.{log2Ceil, Counter, switch, is, Enum, PriorityMux}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.entity.library.WallEntity

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
