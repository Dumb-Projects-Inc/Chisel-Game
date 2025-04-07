package gameEngine.screen

import chisel3._
import chisel3.util._

class PLLBlackBox extends HasBlackBoxPath {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val clk25MHz = Output(Clock())
    val locked = Output(Bool())
  })
  addPath("./src/main/verilog/PLLBlackBox.sv")
}
