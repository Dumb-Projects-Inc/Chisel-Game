package gameEngine

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}
import gameEngine.screen._

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val specs = Seq(
    SpriteSpec("src/main/resources/smiley-64.png", 64, 64),
    SpriteSpec("src/main/resources/finger-48.png", 48, 48),
    SpriteSpec("src/main/resources/DoomUI-640-70.png", 640, 70)
  )
  val posX = RegInit(VecInit(Seq(0.U(10.W), 100.U(10.W), 0.U(10.W))))
  val posY = RegInit(VecInit(Seq(0.U(10.W),  50.U(10.W), 410.U(10.W))))

  val counter = RegInit(0.U(26.W))
  counter := counter + 1.U
  when(counter === (1_000_000 - 1).U) {
    counter := 0.U
    posX(1) := Mux(posX(1) === 0.U, 100.U, posX(1) - 1.U)
  }
  val vgaCtrl = Module(new VGASpriteController(specs))

  io.vga <> vgaCtrl.io.vga

  vgaCtrl.io.pos.x := posX
  vgaCtrl.io.pos.y := posY
}

object gameEngineMain {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).execute(
      Array(
        "--target",     "systemverilog",
        "--target-dir", "generated",
        "--split-verilog"
      ),
      Seq(
        ChiselGeneratorAnnotation(() => new Engine()),
        FirtoolOption("--disable-all-randomization")
      )
    )
  }
}
