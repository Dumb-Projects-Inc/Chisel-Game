package gameEngine.screen

import chisel3._
import chisel3.util._

import gameEngine.util._

class VGAInterface extends Bundle {
  val vsync = Output(Bool())
  val hsync = Output(Bool())
  val red = Output(UInt(4.W))
  val green = Output(UInt(4.W))
  val blue = Output(UInt(4.W))
}

class VGAController extends Module {
  val io = IO(new Bundle {
    val pixel = Input(UInt(12.W))
    val x = Output(UInt(log2Ceil(1024).W))
    val y = Output(UInt(log2Ceil(1024).W))
    val vga = new VGAInterface
  })

  val timing = Module(new VGATiming)
  val visible = timing.io.visible
  io.vga.hsync := timing.io.hSync
  io.vga.vsync := timing.io.vSync

  io.vga.red := Mux(visible, io.pixel(11, 8), 0.U(4.W))
  io.vga.green := Mux(visible, io.pixel(7, 4), 0.U(4.W))
  io.vga.blue := Mux(visible, io.pixel(3, 0), 0.U(4.W))

  io.x := timing.io.pixelX
  io.y := timing.io.pixelY
}
