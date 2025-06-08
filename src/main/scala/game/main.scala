package gameEngine

import chisel3._
import chisel3.util.{log2Ceil, Counter}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.renderer.RainbowRenderer
import gameEngine.entity.SpriteEntity
import gameEngine.entity.library.SmileyEntity
import gameEngine.entity.library.WallEntity
import gameEngine.vec2.Vec2
import gameEngine.framebuffer.Buffer
import chisel3.util.MuxCase

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val controller = Module(new VGAController)
  io.vga := controller.io.vga

  val mem = Module(new Buffer(320, 240, 4, Some("framebuffer_init.mem")))

  mem.io.write := false.B
  mem.io.enable := true.B
  mem.io.dataIn := DontCare
  mem.io.x := controller.io.x / 2.U
  mem.io.y := controller.io.y / 2.U

  val colorPalette = VecInit(
    Seq(
      "h000".U(12.W), //  0: #000000 Black
      "h00A".U(12.W), //  1: #0000AA Blue
      "h0A0".U(12.W), //  2: #00AA00 Green
      "h0AA".U(12.W), //  3: #00AAAA Cyan
      "hA00".U(12.W), //  4: #AA0000 Red
      "hA0A".U(12.W), //  5: #AA00AA Magenta
      "hA50".U(12.W), //  6: #AA5500 Brown
      "hAAA".U(12.W), //  7: #AAAAAA Light Gray
      "h555".U(12.W), //  8: #555555 Dark Gray
      "h55F".U(12.W), //  9: #5555FF Light Blue
      "h5F5".U(12.W), // 10: #55FF55 Light Green
      "h5FF".U(12.W), // 11: #55FFFF Light Cyan
      "hF55".U(12.W), // 12: #FF5555 Light Red
      "hF5F".U(12.W), // 13: #FF55FF Light Magenta
      "hFF5".U(12.W), // 14: #FFFF55 Yellow
      "hFFF".U(12.W) // 15: #FFFFFF White
    )
  )

  val pixelIndex = mem.io.dataOut
  val pixelColor = colorPalette(pixelIndex)
  controller.io.pixel := pixelColor

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
