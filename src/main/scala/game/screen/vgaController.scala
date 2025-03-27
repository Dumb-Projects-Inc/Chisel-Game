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

  val hCounter = RegInit(0.U(10.W))
  val vCounter = RegInit(0.U(10.W))

  val red = RegInit(0.U(4.W))
  val green = RegInit(0.U(4.W))
  val blue = RegInit(0.U(4.W))

  val hsync = WireInit(false.B)
  val vsync = WireInit(false.B)

  val init :: display :: Nil = Enum(2)
  val state = RegInit(init)



  io.red := red
  io.green := green
  io.blue := blue

  io.hsync := hsync
  io.vsync := vsync

  // VGA timing parameters for 640x480 @ 60Hz
  val hVisible = 640.U
  val hFrontPorch = 16.U
  val hSyncPulse = 96.U
  val hBackPorch = 48.U
  val hTotal = hVisible + hFrontPorch + hSyncPulse + hBackPorch

  val vVisible = 480.U
  val vFrontPorch = 11.U
  val vSyncPulse = 2.U
  val vBackPorch = 31.U
  val vTotal = vVisible + vFrontPorch + vSyncPulse + vBackPorch

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
      // 25.175MHz clock signal
      // 640x480 resolution
      // 60Hz refresh rate
      // Horizontal counter
      when(hCounter === hTotal - 1.U) {
        hCounter := 0.U
        when(vCounter === vTotal - 1.U) {
          vCounter := 0.U
        }.otherwise {
          vCounter := vCounter + 1.U
        }
      }.otherwise {
        hCounter := hCounter + 1.U
      }

      // Generate hsync signal (active low)
      hsync := !(hCounter >= (hVisible + hFrontPorch) && hCounter < (hVisible + hFrontPorch + hSyncPulse))

      // Generate vsync signal (active low)
      vsync := !(vCounter >= (vVisible + vFrontPorch) && vCounter < (vVisible + vFrontPorch + vSyncPulse))

      // Set RGB values for a solid red screen
      when(hCounter < hVisible && vCounter < vVisible) {
        red := 8.U // Maximum red intensity
        green := 10.U
        blue := 14.U
      }.otherwise {
        red := 0.U
        green := 0.U
        blue := 0.U
      }
      

    }
  }

}