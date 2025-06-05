package gameEngine.raycast

import chisel3._
import chisel3.util._
import chisel3.util.log2Ceil

import gameEngine.fixed.FixedPointUtils._
import gameEngine.trig.TrigLUT
import gameEngine.vec2._

class RayRequest extends Bundle {
  val start = new Vec2()
  val angle = SInt(32.W)
}

class RayResponse extends Bundle {
  val pos = new Vec2()
  val dist = SInt(32.W)
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

  val currentPosReg = RegInit(Vec2(0.S, 0.S))
  val startPosReg = RegInit(Vec2(0.S, 0.S))
  val hRayReg = RegInit(Vec2(0.S, 0.S))
  val vRayReg = RegInit(Vec2(0.S, 0.S))
  val hRayDeltaReg = RegInit(Vec2(0.S, 0.S))
  val vRayDeltaReg = RegInit(Vec2(0.S, 0.S))
  val stepReg = RegInit(0.U(log2Ceil(maxSteps + 1).W))

  val trig = Module(new TrigLUT)
  trig.io.angle := io.in.bits.angle

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  val currentDist = currentPosReg.dist2(startPosReg)

  io.out.bits.pos := currentPosReg
  io.out.bits.dist := currentDist
  io.out.valid := false.B

  object S extends ChiselEnum {
    val idle, step, done = Value
  }
  val state = RegInit(S.idle)

  io.in.ready := false.B
  switch(state) {
    is(S.idle) {
      io.in.ready := true.B

      when(io.in.fire) {
        val x0 =
          Mux(east, io.in.bits.start.x.fpCeil, io.in.bits.start.x.fpFloor)
        val y0 =
          Mux(north, io.in.bits.start.y.fpCeil, io.in.bits.start.y.fpFloor)

        val hRay0 = Mux(
          horizontal,
          Vec2(MAX, io.in.bits.start.y),
          Vec2(
            io.in.bits.start.x + (y0 - io.in.bits.start.y).fpMul(trig.io.cot),
            y0
          )
        )

        val vRay0 = Mux(
          vertical,
          Vec2(io.in.bits.start.x, MAX),
          Vec2(
            x0,
            io.in.bits.start.y + (x0 - io.in.bits.start.x).fpMul(trig.io.tan)
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

        startPosReg := io.in.bits.start
        stepReg := 0.U

        val hRayDist = hRay0.dist2(io.in.bits.start)
        val vRayDist = vRay0.dist2(io.in.bits.start)
        currentPosReg := Mux(hRayDist < vRayDist, hRay0, vRay0)

        state := S.step

      }
    }
    is(S.step) {
      when(io.stop) {
        state := S.done

      }.elsewhen(stepReg < maxSteps.U) {
        val hRayDist = hRayReg.dist2(startPosReg)
        val vRayDist = vRayReg.dist2(startPosReg)
        val hRayShortest = hRayDist < vRayDist

        val hRayNext = Mux(hRayShortest, hRayReg + hRayDeltaReg, hRayReg)
        val vRayNext = Mux(hRayShortest, vRayReg, vRayReg + vRayDeltaReg)
        hRayReg := hRayNext
        vRayReg := vRayNext

        val nextHdist = hRayNext.dist2(startPosReg)
        val nextVdist = vRayNext.dist2(startPosReg)

        currentPosReg := Mux(nextHdist < nextVdist, hRayNext, vRayNext)

        stepReg := stepReg + 1.U
      }.otherwise {
        state := S.done
      }
    }
    is(S.done) {
      io.out.valid := true.B
      when(io.out.fire) {
        state := S.idle
      }
    }

  }
}
