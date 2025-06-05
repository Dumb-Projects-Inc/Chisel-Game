package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.renderer.RainbowRenderer
import gameEngine.entity.SpriteEntity

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val sprite = Module(new SpriteEntity("./smiley-64.png", 10))
  val controller = Module(new VGAController)
  val rainbow = Module(new RainbowRenderer)
  val counter = RegInit(0.U(26.W))


  sprite.io.in.screen.x := controller.io.x
  sprite.io.in.screen.y := controller.io.y

  sprite.io.in.pos.wrEn := true.B
  val posX = RegInit(100.U(10.W))
  val posY = RegInit(100.U(10.W))

  counter := counter + 1.U
  when(counter === (1_000_000 - 1).U) {
    counter := 0.U
    posX := Mux(posX === 0.U, 100.U, posX - 1.U)
    posY := Mux(posY === 0.U, 100.U, posY - 1.U)
  }

  sprite.io.in.pos.x := posX
  sprite.io.in.pos.y := posY

  sprite.io.in.acceleration.wrEn := false.B
  sprite.io.in.speed.wrEn := false.B

  sprite.io.in.speed.x := DontCare
  sprite.io.in.speed.y := DontCare
  sprite.io.in.acceleration.x := DontCare
  sprite.io.in.acceleration.y := DontCare

  rainbow.io.x := controller.io.x
  rainbow.io.y := controller.io.y

  controller.io.pixel := Mux(
    sprite.io.visible,
    sprite.io.pixel,
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
