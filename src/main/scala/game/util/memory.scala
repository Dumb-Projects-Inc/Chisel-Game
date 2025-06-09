package gameEngine.util

//////////////////////////////////////////////////////////////////////////////
// Authors: Luca Pezzarossa
// Copyright: Technical University of Denmark - 2025
// Comments:
// Wrapper for verilog single port write first memory with initialization
// implemented as Verilog back box.
// Also implemented chisel modules:
// Implementation inspired by the wrapper given from Lucca Pezzarossa
// This implementation intends to behave the same way, but without blackbox
//////////////////////////////////////////////////////////////////////////////

import chisel3._
import chisel3.util.HasBlackBoxResource

// Only works with vivado
class RamInitSpWf(dWidth: Int, addrWidth: Int, loadFile: String)
    extends BlackBox(
      Map(
        "ADDR_WIDTH" -> addrWidth,
        "DATA_WIDTH" -> dWidth,
        "LOAD_FILE" -> loadFile
      )
    )
    with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val we = Input(Bool())
    val en = Input(Bool())
    val addr = Input(UInt(addrWidth.W))
    val di = Input(UInt(dWidth.W))
    val dout = Output(UInt(dWidth.W))
  })
  addResource("/RamInitSpWf.v")
}

//Only works with vivado
class RamSpWf(dWidth: Int, addrWidth: Int)
    extends BlackBox(Map("ADDR_WIDTH" -> addrWidth, "DATA_WIDTH" -> dWidth))
    with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val we = Input(Bool())
    val en = Input(Bool())
    val addr = Input(UInt(addrWidth.W))
    val di = Input(UInt(dWidth.W))
    val dout = Output(UInt(dWidth.W))
  })
  addResource("/RamSpWf.v")
}
