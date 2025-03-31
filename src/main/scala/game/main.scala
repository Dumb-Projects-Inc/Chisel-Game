package gameEngine

import chisel3._
import chisel3.util._
import gameEngine.screen._

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val framebuffer = Module(new FrameBuffer)
  framebuffer.io.address := 0.U
  framebuffer.io.wrEn := false.B
  framebuffer.io.wrData := 0.U
  framebuffer.io.switch := false.B

  val controller = Module(new VGAController)
  io.vga := controller.io
}

object gameEngineMain{
  def main(args: Array[String]): Unit = {
    //Splitting files is expected, and the blackbox will generate syntax errors if not split
    //TODO: change to chiselStage 
    emitVerilog(new Engine(), Array("--target-dir", "generated", "--split-verilog"))
  }
}