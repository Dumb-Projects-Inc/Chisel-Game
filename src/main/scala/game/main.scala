package gameEngine

import chisel3._
import chisel3.util._
//import circt.stage.ChiselStage
import gameEngine.screen._

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val rdAddr = WireInit(0.U(16.W))


  val framebuffer = Module(new FrameBuffer)
  framebuffer.io.wrEn := false.B
  framebuffer.io.wrData := 0.U
  framebuffer.io.address := 0.U
  framebuffer.io.switch := false.B
  val pixelReg = RegNext(framebuffer.io.data)

  val controller = Module(new VGAController)

  when(controller.io.rdAddr === 0.U) {
    framebuffer.io.wrEn := true.B
    framebuffer.io.wrData := pixelReg + 1.U
    framebuffer.io.switch := true.B
  }.otherwise {
    framebuffer.io.wrEn := false.B
    framebuffer.io.wrData := 0.U
    framebuffer.io.switch := false.B
  }
  
  
  //rdAddr := controller.io.rdAddr
  controller.io.pixel := pixelReg
  io.vga := controller.io.vga

}

object gameEngineMain{
  def main(args: Array[String]): Unit = {
    //Splitting files is expected, and the blackbox will generate syntax errors if not split
    //TODO: change to chiselStage
    emitVerilog(new Engine(), Array("--target-dir", "generated", "--split-verilog"))
  }
}