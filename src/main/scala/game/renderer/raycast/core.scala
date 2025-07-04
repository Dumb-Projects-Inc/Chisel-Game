package gameEngine.raycast

import chisel3._
import chisel3.util._

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.pll.PLLBlackBox
import gameEngine.entity.library.WallEntity
import gameEngine.fixed.InverseSqrt
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2

object Defaults {
  val map = Seq(
    Seq(1, 1, 1, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 1, 1)
  )
}

class Column(maxHeight: Int, nTiles: Int) extends Bundle {
  val height = UInt(log2Ceil(maxHeight).W)
  val tile = UInt(log2Ceil(nTiles).W)
  val isHorizontal = Bool()
  val fracHit = UInt(12.W) // Q0.12
}

object Column {
  def apply(maxHeight: Int, nTiles: Int): Column = {
    val w = Wire(new Column(maxHeight, nTiles))
    w.height := 0.U
    w.tile := 0.U
    w.isHorizontal := false.B
    w.fracHit := 0.U(12.W)
    w
  }

  def apply(
      maxHeight: Int,
      nTiles: Int,
      height: UInt,
      tile: UInt,
      isHorizontal: Bool,
      fracHit: UInt
  ): Column = {
    val w = Wire(new Column(maxHeight, nTiles))
    w.height := height
    w.tile := tile
    w.isHorizontal := isHorizontal
    w.fracHit := fracHit
    w
  }
}

/** Wrapper around InverseSqrt which keeps rayhit information
  */
class InverseSqrtStage(nTiles: Int) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RayHit(nTiles)))
    val out = Decoupled(new RayHit(nTiles))
  })

  val invSqrt = Module(new InverseSqrt)

  val rayHitReg = RegInit(RayHit(nTiles))

  object S extends ChiselEnum {
    val ready, emit = Value
  }

  val state = RegInit(S.ready)

  io.in.ready := false.B
  io.out.valid := false.B
  io.out.bits := DontCare
  invSqrt.io.input.valid := io.in.valid
  invSqrt.io.result.ready := io.out.ready
  invSqrt.io.input.bits := io.in.bits.dist
  switch(state) {
    is(S.ready) {
      io.in.ready := invSqrt.io.input.ready
      when(io.in.fire) {
        rayHitReg := io.in.bits
        state := S.emit
      }
    }
    is(S.emit) {
      io.out.valid := invSqrt.io.result.valid
      io.out.bits := RayHit(
        nTiles,
        invSqrt.io.result.bits,
        rayHitReg.tile,
        rayHitReg.isHorizontal,
        rayHitReg.angleOffset,
        rayHitReg.fracHit
      )
      when(io.out.fire) {
        state := S.ready
      }
    }
  }
}

class RaycasterCore(
    map: Seq[Seq[Int]] = Defaults.map,
    width: Int = 320,
    height: Int = 240,
    fov: Double = 1.5
) extends Module {

  val nTiles = map.flatten.max

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RayRequest))
    val columns = Decoupled(Vec(width, new Column(height, nTiles)))
  })

  val rc = Module(
    new RaycastDriver(fov = fov, nRays = width, map = map, nTiles = nTiles)
  )
  val hitQ = Module(new Queue(new RayHit(nTiles), 4))
  val invsqrt = Module(new InverseSqrtStage(nTiles))

  rc.io.response <> hitQ.io.enq
  hitQ.io.deq <> invsqrt.io.in

  io.in.ready := false.B
  invsqrt.io.out.ready := false.B
  io.columns.valid := false.B
  io.columns.bits := DontCare

  object S extends ChiselEnum {
    val ready, calculate, emit = Value
  }

  val state = RegInit(S.ready)

  val idx = RegInit(0.U(log2Ceil(width).W))

  val columns = RegInit(
    VecInit(Seq.fill(width)(Column(height, nTiles)))
  )

  io.in <> rc.io.request
  switch(state) {
    is(S.ready) {
      io.in.ready := true.B
      when(io.in.fire) {
        state := S.calculate
        idx := 0.U
      }
    }
    is(S.calculate) {
      invsqrt.io.out.ready := true.B
      when(invsqrt.io.out.valid) {
        val rayHit = invsqrt.io.out.bits
        val colHeightUnclamped = rayHit.dist.fpMul(toFP(128.0))(23, 12)
        val colHeight =
          Mux(colHeightUnclamped < height.U, colHeightUnclamped, height.U)
        columns(idx) := Column(
          height,
          nTiles,
          colHeight,
          rayHit.tile,
          rayHit.isHorizontal,
          rayHit.fracHit
        )

        idx := idx + 1.U

        when(idx === (width - 1).U) {
          state := S.emit
        }
      }
    }
    is(S.emit) {
      io.columns.valid := true.B
      io.columns.bits := columns
      when(io.columns.ready) {
        state := S.ready
      }
    }
  }
}
