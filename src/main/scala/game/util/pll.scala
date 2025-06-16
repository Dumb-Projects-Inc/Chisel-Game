package gameEngine.pll

import chisel3._
import chisel3.util._

class PLLBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val clk50MHz = Output(Clock())
    val locked = Output(Bool())
  })
  addResource("/Pll.v")
}
