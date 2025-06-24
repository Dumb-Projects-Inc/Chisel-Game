package gameEngine.util

import chisel3._
import chisel3.util._
import gameEngine.vec2.Vec2
import gameEngine.fixed.FixedPointUtils

class UartIO extends DecoupledIO(UInt(8.W))

class posExchange extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new UartIO)
    val out = new UartIO
    val playerPos = Flipped(Decoupled(new Vec2(SInt(FixedPointUtils.width.W))))
    val bobPos = Decoupled(new Vec2(SInt(FixedPointUtils.width.W)))
  })

  val W = FixedPointUtils.width
  val TOTAL_BITS = 2 * W
  val BYTES = (TOTAL_BITS + 7) / 8

  private val WAIT_MSG = VecInit("waiting\n".map(_.U(8.W)))
  private val waitPtr = RegInit(0.U(log2Ceil(WAIT_MSG.length).W))

  object S extends ChiselEnum {
    val idle, send, recv = Value
  }
  val state = RegInit(S.idle)
  val idx = RegInit(0.U(log2Ceil(BYTES + 1).W))
  val sendReg = Reg(UInt(TOTAL_BITS.W))
  val recvReg = Reg(UInt(TOTAL_BITS.W))
  val outVec = Reg(new Vec2(SInt(W.W)))

  io.playerPos.ready := false.B
  io.out.valid := false.B
  io.in.ready := false.B
  io.out.bits := (sendReg >> ((BYTES.U - 1.U - idx) << 3))(7, 0)
  io.bobPos.bits := outVec

  switch(state) {
    is(S.idle) {
      io.playerPos.ready := true.B
      when(io.playerPos.fire) {
        sendReg := Cat(io.playerPos.bits.x.asUInt, io.playerPos.bits.y.asUInt)
        idx := 0.U
        recvReg := 0.U
        io.bobPos.valid := false.B
        state := S.send
      }
    }
    is(S.send) {
      io.out.valid := true.B
      when(io.out.fire) {
        idx := idx + 1.U
        when(idx === (BYTES - 1).U) {
          idx := 0.U
          state := S.recv
        }
      }
    }
    is(S.recv) {
      io.in.ready := true.B
      when(io.in.valid) {
        recvReg := recvReg | (io.in.bits.asUInt << ((BYTES.U - 1.U - idx) << 3))
        idx := idx + 1.U
        when(idx === (BYTES - 1).U) {
          outVec.x := recvReg(TOTAL_BITS - 1, W).asSInt
          outVec.y := recvReg(W - 1, 0).asSInt
          io.bobPos.valid := true.B
          state := S.idle
        }
      }.elsewhen(!io.in.valid) {
        io.out.valid := true.B
        io.out.bits := WAIT_MSG(waitPtr)
        when(io.out.fire) {
          waitPtr := Mux(
            waitPtr === (WAIT_MSG.length - 1).U,
            0.U,
            waitPtr + 1.U
          )
        }
      }
    }
  }

  when(io.bobPos.fire) {
    io.bobPos.valid := false.B
  }
}
