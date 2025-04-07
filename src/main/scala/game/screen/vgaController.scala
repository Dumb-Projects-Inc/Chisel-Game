package gameEngine.screen

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import gameEngine.util._

class VGAInterface(i2cEn: Boolean = false) extends Bundle {
  val vsync = Output(Bool())
  val hsync = Output(Bool())
  val red = Output(UInt(4.W))
  val green = Output(UInt(4.W))
  val blue = Output(UInt(4.W))

  // Data pins for I2C in vga (optional)
  // Note: The basys 3 board does not support I2C in the built-in VGA port
  // Extension board should make this possible.
  val i2c = if (i2cEn) Some(new I2CInterface) else None
}

class VGAController(i2cEn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val pixel = Input(UInt(12.W))
    val rdAddr = Output(UInt(log2Ceil(1024).W))
    val vga = new VGAInterface(i2cEn)
  })

  val visible = Wire(Bool())
  val xPos = Wire(UInt(10.W))
  val yPos = Wire(UInt(10.W))
  val pixel = WireInit("b111111111111".U) // Initialize pixel to white
  pixel := io.pixel

  val pll = Module(new PLLBlackBox)
  pll.io.clock := clock
  pll.io.reset := reset
  val clk25MHz = pll.io.clk25MHz
  val locked = pll.io.locked

  withClockAndReset(clk25MHz, !locked) {
    val timing = Module(new VGATiming)
    io.vga.hsync := timing.io.hSync
    io.vga.vsync := timing.io.vSync
    visible := timing.io.visible
    xPos := timing.io.pixelX
    yPos := timing.io.pixelY

    // Generate RGB signals (example: simple color bars)
    io.vga.red := Mux(visible, pixel(11, 8), 0.U(4.W))
    io.vga.green := Mux(visible, pixel(7, 4), 0.U(4.W))
    io.vga.blue := Mux(visible, pixel(3, 0), 0.U(4.W))

    val rdAddr = Wire(UInt(16.W))
    rdAddr := xPos // Concatenate yPos and xPos to form the address
    io.rdAddr := rdAddr
  }
}
