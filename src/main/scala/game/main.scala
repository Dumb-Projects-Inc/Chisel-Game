package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.renderer.RainbowRenderer
import gameEngine.entity.SpriteEntity
import gameEngine.entity.library.SmileyEntity
import gameEngine.entity.library.WallEntity
import gameEngine.vec2.Vec2

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val controller = Module(new VGAController)
  val rainbow = Module(new RainbowRenderer)

  rainbow.io.x := controller.io.x
  rainbow.io.y := controller.io.y

  controller.io.pixel := rainbow.io.resultPixel
  io.vga := controller.io.vga
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
        ChiselGeneratorAnnotation(() => new Engine()),
        FirtoolOption("--disable-all-randomization")
      )
    )

  }
}
