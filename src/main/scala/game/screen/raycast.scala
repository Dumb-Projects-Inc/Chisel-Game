package gameEngine.screen.raycast

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._
import chisel3.util.log2Ceil

object Vec2 {
  def apply(x: SInt, y: SInt): Vec2 = {
    val v = Wire(new Vec2())
    v.x := x; v.y := y
    v
  }
}

class Vec2(maxSteps: Int = 256) extends Bundle {
  val x, y = SInt(32.W)
}

class Raycaster(maxSteps: Int = 12) extends Module {
  val io = IO(new Bundle {
    val rayStart = Input(new Vec2())
    val rayAngle = Input(SInt(32.W))
    val mapTile = Input(Bool()) // assert when current ray pos is a wall

    val pos = Output(new Vec2())
    val dist = Output(SInt(32.W))

    val valid = Input(Bool())
    val ready = Output(Bool())
  })

  val fire = io.valid & io.ready

  // Constants and helpers
  val one = toFP(1)
  val a0 = toFP(0.0)
  val aPi = toFP(math.Pi)
  val aPiH = toFP(math.Pi / 2)
  val a3PiH = toFP(3 * math.Pi / 2)
  val epsilon = 1.S(32.W)

  val INF = ((BigInt(1) << (width - 1)) - 1).S(width.W)
  val NEG_INF = -INF

  def sqr(x: SInt): SInt = x.fpMul(x)

  val nearTol = toFP(0.005)
  def near(a: SInt, b: SInt): Bool = (a > b - nearTol) && (a < b + nearTol)

  val trig = Module(new gameEngine.trig.TrigLUT)

  // Logic start
  val startReg = RegInit(Vec2(0.S, 0.S))
  val angleReg = RegInit(0.S(32.W))
  trig.io.angle := angleReg

  val pointsEast = angleReg <= aPiH || angleReg > a3PiH
  val pointsSouth = angleReg < aPi

  val hRayReg = RegInit(Vec2(0.S, 0.S))
  val vRayReg = RegInit(Vec2(0.S, 0.S))
  val hDeltaReg = RegInit(Vec2(0.S, 0.S))
  val vDeltaReg = RegInit(Vec2(0.S, 0.S))

  val dH2 = sqr(hRayReg.x - startReg.x) + sqr(hRayReg.y - startReg.y)
  val dV2 = sqr(vRayReg.x - startReg.x) + sqr(vRayReg.y - startReg.y)

  val pos = RegInit(Vec2(0.S, 0.S))
  val distSquared = sqr(pos.x - startReg.x) + sqr(pos.y - startReg.y)

  val step = RegInit(0.U(log2Ceil(maxSteps + 1).W))

  object State extends ChiselEnum {
    val sIdle, sInit, sStep, sCheck, sLoad = Value
  }

  import State._
  val state = RegInit(sIdle)

  when(fire) {
    startReg := io.rayStart
    angleReg := io.rayAngle
    pos := Vec2(0.S, 0.S)
  }

  val is0 = near(angleReg, a0)
  val isPiH = near(angleReg, aPiH)
  val isPi = near(angleReg, aPi)
  val is3PiH = near(angleReg, a3PiH)

  io.ready := false.B
  switch(state) {
    is(sIdle) {
      step := 0.U
      io.ready := true.B
      when(fire) {
        state := sInit
      }
    }

    is(sInit) {

      val tanVal = MuxCase(
        trig.io.tan,
        Seq(
          isPiH -> INF,
          is3PiH -> NEG_INF
        )
      )
      val cotVal = MuxCase(
        trig.io.cot,
        Seq(
          is0 -> INF,
          isPi -> NEG_INF
        )
      )

      // find initial intersection
      val y0 = Mux(pointsSouth, startReg.y.fpCeil, startReg.y.fpFloor)
      val hRay = Vec2(startReg.x + (y0 - startReg.y).fpMul(cotVal), y0)
      hRayReg := hRay

      val x0 = Mux(pointsEast, startReg.x.fpCeil, startReg.x.fpFloor)
      val vRay = Vec2(x0, startReg.y + (x0 - startReg.x).fpMul(tanVal))
      vRayReg := vRay

      // calculate step values
      val hDelta =
        Vec2(
          Mux(pointsSouth, cotVal, -cotVal),
          Mux(pointsSouth, one, -one)
        )
      hDeltaReg := hDelta
      val vDelta =
        Vec2(
          Mux(pointsEast, one, -one),
          Mux(pointsEast, tanVal, -tanVal)
        )
      vDeltaReg := vDelta

      state := sLoad
    }

    is(sLoad) {
      when(dH2 <= dV2) {
        pos := hRayReg
      }.otherwise {
        pos := vRayReg
      }

      state := sCheck
    }

    is(sStep) {
      step := step + 1.U

      // calculate and step shortest ray
      when(dH2 <= dV2) {
        hRayReg := Vec2(hRayReg.x + hDeltaReg.x, hRayReg.y + hDeltaReg.y)
      }.otherwise {
        vRayReg := Vec2(vRayReg.x + vDeltaReg.x, vRayReg.y + vDeltaReg.y)
      }

      state := sLoad

    }

    is(sCheck) {
      when(io.mapTile || step >= maxSteps.U) {
        state := sIdle
      }.otherwise {
        state := sStep
      }
    }
  }

  io.pos := pos
  io.dist := distSquared
}
