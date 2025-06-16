package gameEngine.raycast

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.trig.TrigLUT
import gameEngine.vec2._
import gameEngine.vec2.Vec2._



class RayHit(nTiles: Int) extends Bundle {
  val dist = SInt(24.W)
  val tile = UInt(log2Ceil(nTiles).W)
  val isHorizontal = Bool()
  val angleOffset = SInt(24.W)
}

object RayHit {
  def apply(nTiles: Int) = {
    val w = Wire(new RayHit(nTiles))
    w.dist := 0.S(24.W)
    w.tile := 0.U(log2Ceil(nTiles).W)
    w.isHorizontal := false.B
    w.angleOffset := 0.S(24.W)
    w
  }

  def apply(
      nTiles: Int,
      dist: SInt,
      tile: UInt,
      isHorizontal: Bool,
      angleOffset: SInt
  ) = {
    val w = Wire(new RayHit(nTiles))
    w.dist := dist
    w.tile := tile
    w.isHorizontal := isHorizontal
    w.angleOffset := angleOffset
    w
  }
}

class RaycastDriver(
    fov: Double = 2,
    nRays: Int = 12,
    nTiles: Int = 2,
    map: Seq[Seq[Int]]
) extends Module {
  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new RayRequest))
    val response = Decoupled(new RayHit(nTiles))
  })

  def near(a: SInt, b: SInt, tol: Double = 0.001): Bool = {
    val diff = a - b
    val absDiff = Mux(diff >= 0.S, diff, -diff)
    absDiff <= toFP(tol)
  }

  val offsets: Seq[Double] =
    if (nRays > 1) {
      val halfFov = fov / 2.0
      val step = fov / (nRays - 1)
      (0 until nRays).map(i => step * i - halfFov)
    } else {
      Seq(0.0)
    }
  val offsetsVec = VecInit.tabulate(nRays) { (i) => (toFP(offsets(i))) }

  val mapVec = VecInit.tabulate(map.length, map(0).length) { (x, y) =>
    map(x)(y).U
  }

  val raycaster = Module(new Raycaster)

  val trig = Module(new TrigLUT)
  val angleReg = RegInit(0.S(24.W))
  trig.io.angle := angleReg

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  val requestReg = RegInit(
    (new RayRequest)
      .Lit(
        _.start -> ((new Vec2(SInt(24.W)))
          .Lit(_.x -> 0.S(24.W), _.y -> 0.S(24.W))),
        _.angle -> 0.S(24.W)
      )
  )

  val currentRayOffsetIdx = RegInit(0.U(log2Ceil(nRays + 1).W))
  val currentRayPos = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val currentRayDist = RegInit(0.S(24.W))
  val currentRayHorizontal = RegInit(false.B)
  val currentRayTile = RegInit(0.U(log2Ceil(nTiles).W))
  val currentRayAngleOffset = RegInit(0.S(24.W))

  object S extends ChiselEnum {
    val idle, initRay, step, check, emit = Value
  }
  val state = RegInit(S.idle)

  io.request.ready := false.B
  raycaster.io.out.ready := false.B
  raycaster.io.stop := false.B

  raycaster.io.in.valid := false.B
  raycaster.io.in.bits := DontCare

  io.response.valid := false.B
  io.response.bits.dist := DontCare
  io.response.bits.tile := DontCare
  io.response.bits.angleOffset := DontCare
  io.response.bits.isHorizontal := DontCare

  switch(state) {
    is(S.idle) {
      io.request.ready := true.B

      when(io.request.fire) {
        requestReg := io.request.bits
        currentRayOffsetIdx := 0.U
        state := S.initRay
      }
    }
    is(S.initRay) {
      raycaster.io.in.bits.start := requestReg.start
      val angleOffset = offsetsVec(currentRayOffsetIdx)
      val angle = requestReg.angle + angleOffset
      raycaster.io.in.bits.angle := angle
      angleReg := angle
      raycaster.io.in.valid := true.B
      currentRayAngleOffset := angleOffset
      when(raycaster.io.in.ready) {
        currentRayOffsetIdx := currentRayOffsetIdx + 1.U
        state := S.step
      }
    }
    is(S.step) {
      when(raycaster.io.out.valid) {
        currentRayPos := raycaster.io.out.bits.pos
        currentRayHorizontal := raycaster.io.out.bits.isHorizontal
        currentRayDist := raycaster.io.out.bits.dist
        state := S.check
      }
    }
    is(S.check) {
      raycaster.io.out.ready := true.B
      val hitIdx = {
        val idxFP = Mux(
          currentRayHorizontal,
          Mux(
            north,
            Vec2(currentRayPos.x.fpFloor, currentRayPos.y),
            Vec2(currentRayPos.x.fpFloor, currentRayPos.y - toFP(1.0))
          ),
          Mux(
            east,
            Vec2(currentRayPos.x, currentRayPos.y.fpFloor),
            Vec2(currentRayPos.x - toFP(1.0), currentRayPos.y.fpFloor)
          )
        )
        Vec2(idxFP.x(23, 12), idxFP.y(23, 12))
      }
      val tileHit = mapVec(hitIdx.y)(hitIdx.x)
      when(tileHit =/= 0.U) {
        raycaster.io.stop := true.B
        currentRayTile := tileHit
        state := S.emit
      }.otherwise {
        state := S.step
      }
    }
    is(S.emit) {
      io.response.bits.dist := currentRayDist
      io.response.bits.tile := currentRayTile
      io.response.bits.isHorizontal := currentRayHorizontal
      io.response.bits.angleOffset := currentRayAngleOffset
      io.response.valid := true.B
      when(io.response.ready) {
        when(currentRayOffsetIdx === nRays.U) {
          state := S.idle
        }.otherwise {
          state := S.initRay
        }
      }
    }
  }
}
