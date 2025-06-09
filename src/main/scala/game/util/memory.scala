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
import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType

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

class InitSinglePortWriteFirstRAM(dWidth: Int, addrWidth: Int, loadFile: String)
    extends Module {
  val io = IO(new Bundle {
    val we = Input(Bool())
    val en = Input(Bool())
    val addr = Input(UInt(addrWidth.W))
    val di = Input(UInt(dWidth.W))
    val dout = Output(UInt(dWidth.W))
  })

  val ram = SyncReadMem(1 << addrWidth, UInt(dWidth.W))
  val doutReg = RegInit(0.U(dWidth.W))
  loadMemoryFromFile(ram, loadFile)

  when(io.en) {
    when(io.we) {
      ram.write(io.addr, io.di)
      doutReg := io.di
    }.otherwise {
      doutReg := ram.read(
        io.addr,
        true.B
      )
    }
  }

  io.dout := doutReg
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

class SinglePortWriteFirstRAM(dWidth: Int, addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val we = Input(Bool())
    val en = Input(Bool())
    val addr = Input(UInt(addrWidth.W))
    val di = Input(UInt(dWidth.W))
    val dout = Output(UInt(dWidth.W))
  })

  val ram = SyncReadMem(1 << addrWidth, UInt(dWidth.W))
  val doutReg = RegInit(0.U(dWidth.W))

  when(io.en) {
    when(io.we) {
      ram.write(io.addr, io.di)
      doutReg := io.di
    }.otherwise {
      doutReg := ram.read(
        io.addr,
        true.B
      )
    }
  }

  io.dout := doutReg
}
