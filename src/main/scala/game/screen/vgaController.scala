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

  // VGA timing parameters for 640x480 @ 60Hz
  val hVisible = 640
  val hFrontPorch = 16
  val hSyncPulse = 96
  val hBackPorch = 48
  val hTotal = hVisible + hFrontPorch + hSyncPulse + hBackPorch

  val vVisible = 480
  val vFrontPorch = 10
  val vSyncPulse = 2
  val vBackPorch = 33
  val vTotal = vVisible + vFrontPorch + vSyncPulse + vBackPorch

  // Horizontal and vertical counters
  val hCounter = RegInit(0.U(log2Ceil(hTotal).W))
  val vCounter = RegInit(0.U(log2Ceil(vTotal).W))

  // Horizontal counter logic
  when(hCounter === (hTotal - 1).U) {
    hCounter := 0.U
    when(vCounter === (vTotal - 1).U) {
      vCounter := 0.U
    }.otherwise {
      vCounter := vCounter + 1.U
    }
  }.otherwise {
    hCounter := hCounter + 1.U
  }

  // Generate sync signals
  io.hsync := !(hCounter >= (hVisible + hFrontPorch).U && hCounter < (hVisible + hFrontPorch + hSyncPulse).U)
  io.vsync := !(vCounter >= (vVisible + vFrontPorch).U && vCounter < (vVisible + vFrontPorch + vSyncPulse).U)

  // Generate RGB signals (example: simple color bars)
  io.red := Mux(hCounter < (hVisible / 3).U, 15.U, 0.U)
  io.green := Mux(hCounter >= (hVisible / 3).U && hCounter < (2 * hVisible / 3).U, 15.U, 0.U)
  io.blue := Mux(hCounter >= (2 * hVisible / 3).U, 15.U, 0.U)

}