package gameEngine

import chisel3._
import chisel3.util._
import gameEngine.screen._

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val pixelClock = Input(Bool())
  })

  val framebuffer = Module(new FrameBuffer)
  framebuffer.io.address := 0.U
  framebuffer.io.wrEn := false.B
  framebuffer.io.wrData := 0.U
  framebuffer.io.switch := false.B


  val pll = Module(new PLLBlackBox)
  pll.io.clock := clock
  pll.io.reset := false.B
  val clk25MHz = pll.io.clk25MHz
  val locked = pll.io.locked


  withClockAndReset(clk25MHz, !locked) {
    val controller = Module(new VGAController)
    io.vga := controller.io

  }

  

}

object gameEngineMain {
  def main(args: Array[String]): Unit = {
    emitVerilog(new Engine(), Array("--target-dir", "generated", "--split-verilog"))
  }
}