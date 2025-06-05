package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.renderer.RainbowRenderer
import gameEngine.sprite.ImageSprite

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val sprite = Module(new ImageSprite("./smiley-64.png", 64, 64))
  val controller = Module(new VGAController)
  val rainbow = Module(new RainbowRenderer)

  val visible =
    controller.io.x < 64.U && controller.io.y < 64.U && !sprite.io.transparent

  sprite.io.x := controller.io.x
  sprite.io.y := controller.io.y

  rainbow.io.x := controller.io.x
  rainbow.io.y := controller.io.y

  controller.io.pixel := Mux(
    visible,
    sprite.io.r ## sprite.io.g ## sprite.io.b,
    rainbow.io.resultPixel
  )

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
