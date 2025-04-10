package gameEngine.renderer

import chisel3._
import chisel3.util.{Cat, log2Ceil, switch, is}

class RainbowRenderer extends Renderer {
  // 60 fps 
  val counter = RegInit(0.U(log2Ceil(1_666_666).W))
  val colorOffset = RegInit(0.U(8.W))
  counter := counter + 1.U

  when(counter === 0.U) {
    colorOffset := colorOffset + 1.U
  }

  val hue = (io.x+io.y + colorOffset)(7,0)

  // Divide hue into 6 regions (each ~43 wide)
  val region = (hue / 43.U)(2, 0) // 0 to 5
  val offset = ((hue % 43.U) * 6.U)(7,0) // for gradient within region

  val r = WireInit(0.U(8.W))
  val g = WireInit(0.U(8.W))
  val b = WireInit(0.U(8.W))
  switch(region) {
    is(0.U) { r := 255.U;     g := offset(7, 0); b := 0.U     } // Red → Yellow
    is(1.U) { r := 255.U - offset(7, 0); g := 255.U; b := 0.U } // Yellow → Green
    is(2.U) { r := 0.U;       g := 255.U; b := offset(7, 0)   } // Green → Cyan
    is(3.U) { r := 0.U;       g := 255.U - offset(7, 0); b := 255.U } // Cyan → Blue
    is(4.U) { r := offset(7, 0); g := 0.U; b := 255.U         } // Blue → Magenta
    is(5.U) { r := 255.U;     g := 0.U; b := 255.U - offset(7, 0) } // Magenta → Red
  }

  // Convert 8-bit to 4-bit RGB
  io.resultPixel := Cat(r(7, 4), g(7, 4), b(7, 4))
}
