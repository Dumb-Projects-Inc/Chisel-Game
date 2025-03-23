package gameEngine.screen

import chisel3._

class FrameBuffer extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(16.W))
    val data = Output(UInt(16.W))
    val wrEn = Input(Bool())
    val wrData = Input(UInt(16.W))
    val switch = Input(Bool())
  })

  // Each address can store red, green, and blue values
  val frameBuf0 = SyncReadMem(65536, UInt(12.W))
  val frameBuf1 = SyncReadMem(65536, UInt(12.W))

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

  //read from the framebuffer that cannot be written to
  io.data := Mux(framebufSel, frameBuf1.read(io.address), frameBuf0.read(io.address))

}