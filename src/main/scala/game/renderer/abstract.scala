package gameEngine.renderer

import chisel3._
import chisel3.util.log2Ceil

class RendererInterface extends Bundle {
  val x = Input(UInt(log2Ceil(1024).W))
  val y = Input(UInt(log2Ceil(768).W))
  val resultPixel = Output(UInt(12.W)) // TODO: change to rgb struct
}
