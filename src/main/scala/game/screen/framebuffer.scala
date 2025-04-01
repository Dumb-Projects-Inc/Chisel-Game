package gameEngine.screen

import chisel3._

import chisel3.util.experimental.loadMemoryFromFileInline
import scala.io.Source


class FrameBuffer(size: Int = 65536) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(16.W))
    val data = Output(UInt(12.W))
    val wrEn = Input(Bool())
    val wrData = Input(UInt(12.W))
    val switch = Input(Bool())
  })

  // Each address can store red, green, and blue values
  val frameBuf0 = SyncReadMem(size, UInt(12.W))
  val frameBuf1 = SyncReadMem(size, UInt(12.W))

  val framebufSel = RegInit(false.B)

  when(io.switch) {
    framebufSel := ~framebufSel
  }

  //Write to the framebuffer that cannot be read from
  when(io.wrEn) {
    when(framebufSel) {
      frameBuf0.write(io.address, io.wrData)
    }.otherwise {
      frameBuf1.write(io.address, io.wrData)
    }
  }

  val data = Wire(UInt(12.W))
  val rdEn = !io.wrEn

  data := 0x759.U(12.W)
  when(rdEn){
    //read from the framebuffer that cannot be written to
    data := Mux(framebufSel, frameBuf1.read(io.address), frameBuf0.read(io.address))
  }
  
  io.data := RegNext(data)
}