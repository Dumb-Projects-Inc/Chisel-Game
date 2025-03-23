package gameEngine

import chisel3._
import gameEngine.screen._

class MyModule extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(16.W))
    val out = Output(UInt(16.W))
  })

  val controller = Module(new VGAController)
  val framebuffer = Module(new FrameBuffer)
  framebuffer.io.address := 0.U
  framebuffer.io.wrEn := false.B
  framebuffer.io.wrData := 0.U
  framebuffer.io.switch := false.B

  val red = RegInit(0.U(4.W))
  val green = RegInit(0.U(4.W))
  val blue = RegInit(0.U(4.W))

  controller.io.red :>= red
  controller.io.green :>= green
  controller.io.blue :>= blue

  val hsync = RegInit(false.B)
  val vsync = RegInit(false.B)

  controller.io.hsync :>= hsync
  controller.io.vsync :>= vsync



  io.out := RegNext(io.in)
}

object gameEngineMain {
  def main(args: Array[String]): Unit = {
    emitVerilog(new MyModule())
  }
}