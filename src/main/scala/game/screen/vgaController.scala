package gameEngine.screen

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import gameEngine.util._

class VGAInterface(i2cEn: Boolean = false ) extends Bundle {
  val vsync = Output(Bool())
  val hsync = Output(Bool())
  val red = Output  (UInt(4.W))
  val green = Output(UInt(4.W))
  val blue = Output (UInt(4.W))

  // Data pins for I2C in vga (optional)
  // Note: The basys 3 board does not support I2C in the built-in VGA port
  // Extension board should make this possible.
  val i2c = if (i2cEn) Some(new I2CInterface) else None
}

class VGAController(i2cEn: Boolean = false ) extends Module {
  val io = IO(new VGAInterface(i2cEn))

  val red = RegInit(0.U(4.W))
  val green = RegInit(0.U(4.W))
  val blue = RegInit(0.U(4.W))

  val hsync = RegInit(false.B)
  val vsync = RegInit(false.B)

  val init :: display :: Nil = Enum(2)
  val state = RegInit(init)

  val hCounter = RegInit(0.U(10.W))
  val vCounter = RegInit(0.U(10.W))

  io.red := red
  io.green := green
  io.blue := blue

  io.hsync := hsync
  io.vsync := vsync


  switch(state) {
    is(init) {
      if(i2cEn) {
        // Negotiate with the monitor to get the correct resolution and refresh rate
        
        // Initialize I2C controller
        val i2c = Module(new I2C)
        i2c.io.sda <> io.i2c.get.sda
        i2c.io.scl <> io.i2c.get.scl
        
        // Send the I2C signals to the monitor

        // Wait for the monitor to respond

        // Fallback resolution

      } else {
        // Initialize the VGA controller
        // Set the resolution to 640x480
        // Set the refresh rate to 60Hz
        

        // Requires a 25.175MHz clock signal



      }

      state := display
    }
    is(display) {

    }
  }

}