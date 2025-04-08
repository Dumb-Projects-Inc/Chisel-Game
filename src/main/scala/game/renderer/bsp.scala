package gameEngine.renderer

import chisel3._

class BSPRenderer extends Renderer {

  val buffer = Module(new LineBuffer(1024))
  io.resultPixel := "b110000000000".U(12.W)

  buffer.io.switch := Mux(io.x === 0.U, true.B, false.B)
  //TODO: Make it
}