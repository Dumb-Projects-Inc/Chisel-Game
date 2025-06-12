package gameEngine.raycast

import chisel3._
import chisel3.Mux
import chisel3.util._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.trig.TrigLUT
import gameEngine.vec2._
import gameEngine.vec2.Vec2._

class RayRequest extends Bundle {
  val start = new Vec2(SInt(24.W))
  val angle = SInt(24.W)
}

class RayResponse extends Bundle {
  val pos = new Vec2(SInt(24.W))
  val dist = SInt(24.W)
  val isHorizontal = Bool()
}

class Raycaster(maxSteps: Int = 12) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RayRequest))
    val out = Decoupled(new RayResponse)
    val stop = Input(Bool()) // assert to stop the ray
  })

  def near(a: SInt, b: SInt, tol: Double = 0.001): Bool = {
    val diff = a - b
    val absDiff = Mux(diff >= 0.S, diff, -diff)
    absDiff <= toFP(tol)
  }

  val trig = Module(new TrigLUT)
  val angleReg = RegInit(0.S(24.W))
  trig.io.angle := angleReg

  val currentPosReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val startPosReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val hRayReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val vRayReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val hRayDeltaReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val vRayDeltaReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val stepReg = RegInit(0.U(log2Ceil(maxSteps + 1).W))

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  val currentDist = currentPosReg.dist2Fp(startPosReg)
  val hRayDist = hRayReg.dist2Fp(startPosReg)
  val vRayDist = vRayReg.dist2Fp(startPosReg)
  val currentHitIsHorizontal = hRayDist < vRayDist

  io.out.bits.pos := currentPosReg
  io.out.bits.dist := currentDist
  io.out.bits.isHorizontal := currentHitIsHorizontal
  io.out.valid := false.B

  object S extends ChiselEnum {
    val idle, init0, init1, init2, init3, step1, step2, done = Value
  }
  val state = RegInit(S.idle)

  io.in.ready := false.B

  val x0 =
    Mux(east, startPosReg.x.fpCeil, startPosReg.x.fpFloor)
  val y0 =
    Mux(north, startPosReg.y.fpCeil, startPosReg.y.fpFloor)

  val intermediateProduct = RegInit(0.S(24.W))
  val intermediateProduct1 = RegInit(0.S(24.W))

  switch(state) {
    is(S.idle) {
      io.in.ready := true.B

      when(io.in.fire) {
        angleReg := io.in.bits.angle
        startPosReg := io.in.bits.start
        state := S.init0
      }
    }
    is(S.init0) {
      state := S.init1
    }
    is(S.init1) {
      intermediateProduct := (y0 - startPosReg.y).fpMul(trig.io.cot)
      intermediateProduct1 := (x0 - startPosReg.x).fpMul(trig.io.tan)
      state := S.init2

    }
    is(S.init2) {
      state := S.init3
      val x0 =
        Mux(east, startPosReg.x.fpCeil, startPosReg.x.fpFloor)
      val y0 =
        Mux(north, startPosReg.y.fpCeil, startPosReg.y.fpFloor)

      val hRay0 = Mux(
        horizontal,
        Vec2(MAX, startPosReg.y),
        Vec2(
          startPosReg.x + intermediateProduct,
          y0
        )
      )

      val vRay0 = Mux(
        vertical,
        Vec2(startPosReg.x, MAX),
        Vec2(
          x0,
          startPosReg.y + intermediateProduct1
        )
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

    }
    is(S.init3) {
      stepReg := 0.U

      val hRayDist0 = hRayReg.dist2Fp(startPosReg)
      val vRayDist0 = vRayReg.dist2Fp(startPosReg)
      currentPosReg := Mux(hRayDist0 < vRayDist0, hRayReg, vRayReg)

      state := S.step1
    }
    is(S.step1) {
      when(io.stop) {
        state := S.done

      }.elsewhen(stepReg < maxSteps.U) {
        val hRayNext =
          Mux(currentHitIsHorizontal, hRayReg + hRayDeltaReg, hRayReg)
        val vRayNext =
          Mux(currentHitIsHorizontal, vRayReg, vRayReg + vRayDeltaReg)

        hRayReg := hRayNext
        vRayReg := vRayNext
        state := S.step2

      }.otherwise {
        state := S.done
      }
    }
    is(S.step2) {
      val nextHdist = hRayReg.dist2Fp(startPosReg)
      val nextVdist = vRayReg.dist2Fp(startPosReg)

      currentPosReg := Mux(nextHdist < nextVdist, hRayReg, vRayReg)

      stepReg := stepReg + 1.U

      state := S.step1
    }
    is(S.done) {
      io.out.valid := true.B
      when(io.out.fire) {
        state := S.idle
      }
    }
  }
}
