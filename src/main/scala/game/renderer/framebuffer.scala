package gameEngine.framebuffer

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

class Buffer(
    width: Int,
    height: Int,
    elementWidth: Int,
    memoryFile: Option[String] = None
) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val x = Input(UInt(log2Ceil(width).W))
    val y = Input(UInt(log2Ceil(height).W))
    val dataIn = Input(UInt(elementWidth.W))
    val dataOut = Output(UInt(elementWidth.W))
  })

  val addr = io.y * width.U + io.x

  if (memoryFile.isDefined) {
    val m = Module(
      new gameEngine.util.RamInitSpWf(
        elementWidth,
        addr.getWidth,
        memoryFile.get
      )
    )
    m.io.clk := clock
    m.io.en := io.enable
    m.io.we := io.write
    m.io.addr := addr
    m.io.di := io.dataIn
    io.dataOut := m.io.dout

  } else {
    val m = Module(new gameEngine.util.RamSpWf(elementWidth, addr.getWidth))
    m.io.clk := clock
    m.io.en := io.enable
    m.io.we := io.write
    m.io.addr := addr
    m.io.di := io.dataIn
    io.dataOut := m.io.dout
  }

}
