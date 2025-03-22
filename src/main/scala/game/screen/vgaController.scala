package gameEngine.screen

import chisel3._

class VGAInterface(i2c = false ) extends Bundle {
  val in = Input(UInt(16.W))
  val vsync = Output(Bool())
  val hsync = Output(Bool())
  val red = Output  (UInt(4.W))
  val green = Output(UInt(4.W))
  val blue = Output (UInt(4.W))

  //Data pins for I2C in vga (optional)
  if(i2c){
    val sda = Output(Bool()) //pin 12
    val scl = Output(Bool()) //pin 15
  }
}

class VGAController extends Module {
  val io = IO(new VGAInterface)

  val red = RegInit(0.U(4.W))
  val green = RegInit(0.U(4.W))
  val blue = RegInit(0.U(4.W))

  val hsync = RegInit(false.B)
  val vsync = RegInit(false.B)
}