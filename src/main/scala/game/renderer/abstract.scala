package gameEngine.renderer

import chisel3._
import chisel3.util.log2Ceil


class RendererInterface extends Bundle {
  val x = Input(UInt(log2Ceil(1024).W)) 
  val y = Input(UInt(log2Ceil(768).W)) 
  val resultPixel = Output(UInt(12.W)) //TODO: change to rgb struct
}
/** Abstract Renderer module.
  * Request a color at x,y value by driving inputs.
  */
class Renderer extends Module {
  val io = IO(new RendererInterface)
  

  io.resultPixel := "b110000000000".U(12.W)

}