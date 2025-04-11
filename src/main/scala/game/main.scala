package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val rdAddr = WireInit(0.U(log2Ceil(1024).W))

  val linebuffer = Module(new LineBuffer)
  linebuffer.io.wrAddr := 0.U
  linebuffer.io.wrData := DontCare
  linebuffer.io.address := rdAddr
  linebuffer.io.switch := false.B
  val pixelReg = linebuffer.io.data

  // Init framebuffer with a pattern
  // in a state machine -

  val init = RegInit(0.U(32.W))
  val doneRender = RegInit(false.B)
  when(!doneRender) {
    when(init < 640.U) {
      linebuffer.io.wrAddr := init(11, 0)
      linebuffer.io.wrData := init(11, 0)
      init := init + 1.U
    }.otherwise {
      linebuffer.io.wrAddr := 0.U
      linebuffer.io.switch := true.B
      doneRender := true.B
    }
  }.otherwise {
    linebuffer.io.switch := false.B
  }

  val controller = Module(new VGAController)

  rdAddr := controller.io.rdAddr
  controller.io.pixel := pixelReg
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
