package gameEngine.renderer

import chisel3._
import chisel3.util.{Cat, log2Ceil}

class RainbowRenderer extends Renderer {
  val counter = RegInit(0.U(log2Ceil(25_000_000).W))
  val colorOffset = RegInit(0.U(8.W))
  counter := counter + 1.U

  when(counter === 0.U) {
    colorOffset := colorOffset + 1.U
  }

  val red = ((io.x + colorOffset) * 3.U) % 256.U
  val green = ((io.x + colorOffset) * 5.U) % 256.U
  val blue = ((io.x + colorOffset) * 7.U) % 256.U

  // Combine RGB values into a single pixel value
  io.resultPixel := Cat(red(7, 4), green(7, 4), blue(7, 4))
}
