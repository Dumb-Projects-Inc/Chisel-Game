package gameEngine.screen

import chisel3._
import chisel3.util._


class FrameBuffer(size: Int = 640) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(1024).W))
    val data = Output(UInt(12.W))
    val wrEn = Input(Bool())
    val wrData = Input(UInt(12.W))
    val switch = Input(Bool())
  })

  // Each address can store red, green, and blue values
  val frameBuf0 = SyncReadMem(size, UInt(12.W))
  when(io.wrEn){
      frameBuf0.write(io.address, io.wrData)
  }

  val data = Wire(UInt(12.W))

  data := frameBuf0.read(io.address, !io.wrEn)
  
  io.data := RegNext(data)
}