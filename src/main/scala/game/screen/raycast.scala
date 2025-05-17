package gameEngine.screen.raycast

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._
import chisel3.util.log2Ceil
import gameEngine.trig.TrigLUT
import gameEngine.fixed.FixedPointUtils

object Vec2 {
  def apply(x: SInt, y: SInt): Vec2 = {
    val v = Wire(new Vec2())
    v.x := x; v.y := y
    v
  }
}

class Vec2(maxSteps: Int = 256) extends Bundle {
  val x, y = SInt(32.W)

  def +(that: Vec2): Vec2 = {
    Vec2(this.x + that.x, this.y + that.y)
  }
}

class Raycaster(maxSteps: Int = 12) extends Module {
  val io = IO(new Bundle {
    val rayStart = Input(new Vec2())
    val rayAngle = Input(SInt(32.W))
    val stop = Input(Bool()) // assert to stop the ray

    val pos = Output(new Vec2())
    val dist = Output(SInt(32.W))

    val valid = Input(Bool())
    val ready = Output(Bool())
  })
  val MAX = FixedPointUtils.maxVal

  def near(a: SInt, b: SInt, tol: Double = 0.001): Bool = {
    val diff = a - b
    val absDiff = Mux(diff >= 0.S, diff, -diff)
    absDiff <= toFP(tol)
  }

  def sqr(a: SInt): SInt = {
    a.fpMul(a)
  }

  def dist2(v: Vec2): SInt =
    sqr(v.x - startPosReg.x) +& sqr(v.y - startPosReg.y)

  val angleReg = RegInit(0.S(32.W))
  val currentPosReg = RegInit(Vec2(0.S, 0.S))
  val startPosReg = RegInit(Vec2(0.S, 0.S))
  val hRayReg = RegInit(Vec2(0.S, 0.S))
  val vRayReg = RegInit(Vec2(0.S, 0.S))
  val hRayDeltaReg = RegInit(Vec2(0.S, 0.S))
  val vRayDeltaReg = RegInit(Vec2(0.S, 0.S))
  val stepReg = RegInit(0.U(log2Ceil(maxSteps + 1).W))

  val hRayDist = dist2(hRayReg)
  val vRayDist = dist2(vRayReg)
  val currentDist = dist2(currentPosReg)

  io.pos := currentPosReg
  io.dist := currentDist

  val trig = Module(new TrigLUT)
  trig.io.angle := io.rayAngle

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  object S extends ChiselEnum {
    val idle, init, load, check, step = Value
  }
  val state = RegInit(S.idle)

  io.ready := false.B
  switch(state) {
    is(S.idle) {
      io.ready := true.B

      when(io.valid) {
        val x0 = Mux(east, io.rayStart.x.fpCeil, io.rayStart.x.fpFloor)
        val y0 = Mux(north, io.rayStart.y.fpCeil, io.rayStart.y.fpFloor)

        val hRay0 = Mux(
          horizontal,
          Vec2(MAX, io.rayStart.y),
          Vec2(io.rayStart.x + (y0 - io.rayStart.y).fpMul(trig.io.cot), y0)
        )

        val vRay0 = Mux(
          vertical,
          Vec2(io.rayStart.x, MAX),
          Vec2(x0, io.rayStart.y + (x0 - io.rayStart.x).fpMul(trig.io.tan))
        )

        val hRayDelta = Vec2(
          MuxCase(
            Mux(north, trig.io.cot, -trig.io.cot),
            Seq(
              vertical -> 0.S,
              horizontal -> MAX
            )
          ),
          Mux(north, toFP(1), toFP(-1))
        )

        val vRayDelta = Vec2(
          Mux(east, toFP(1), toFP(-1)),
          MuxCase(
            Mux(east, trig.io.tan, -trig.io.tan),
            Seq(vertical -> MAX, horizontal -> 0.S)
          )
        )

        hRayReg := hRay0
        vRayReg := vRay0
        hRayDeltaReg := hRayDelta
        vRayDeltaReg := vRayDelta

        startPosReg := io.rayStart
        stepReg := 0.U
        state := S.load
      }
    }
    is(S.load) {
      when(hRayDist < vRayDist) {
        currentPosReg := hRayReg
      }.otherwise {
        currentPosReg := vRayReg
      }

      state := S.check
    }
    is(S.check) {
      when(io.stop | stepReg >= maxSteps.U) {
        state := S.idle
      }.otherwise {
        state := S.step
      }
    }
    is(S.step) {
      stepReg := stepReg + 1.U

      when(hRayDist < vRayDist) {
        hRayReg := hRayReg + hRayDeltaReg
      }.otherwise {
        vRayReg := vRayReg + vRayDeltaReg
      }

      state := S.load
    }
  }

}
