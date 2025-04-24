package gameEngine.renderer

import chisel3._
import chisel3.util._

class LineBuffer(size: Int = 640, debug: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(1024).W))
    val data = Output(UInt(12.W))
    val wrAddr = Input(UInt(log2Ceil(1024).W))
    val wrData = Input(UInt(12.W))
    val switch = Input(Bool())
    val bufferSel = if (debug) Some(Output(Bool())) else None
  })

  // Each address can store red, green, and blue values
  val linebuf0 = SyncReadMem(size, UInt(12.W))
  val linebuf1 = SyncReadMem(size, UInt(12.W))

  val bufferSel = RegInit(false.B)
  if (debug) {
    io.bufferSel.get := bufferSel
  }
  val writeEn = Wire(Bool())
  val data = Wire(UInt(12.W))
  writeEn := io.wrAddr =/= 0.U

  // Switch buffer on switch signal, usually by line end
  val prevReg = RegNext(io.switch)
  val risingEdge = io.switch && !prevReg
  when(risingEdge) {
    bufferSel := ~bufferSel
  }

  when(writeEn) {
    when(bufferSel) {
      linebuf1.write(io.wrAddr, io.wrData)
    }.otherwise {
      linebuf0.write(io.wrAddr, io.wrData)
    }
  }

  // Opposite of write target
  when(bufferSel) {
    data := linebuf0.read(io.address, true.B)
  }.otherwise {
    data := linebuf1.read(io.address, true.B)
  }

  io.data := data
}
