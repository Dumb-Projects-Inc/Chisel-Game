package gameEngine.entity

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._
import gameEngine.fixed.FixedPointUtils
import gameEngine.fixed.InverseSqrt
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class CircleEntity(fov: Double = 1.5) extends Module {
  val io = IO(new Bundle {
    val input = Flipped(Decoupled(new Bundle {
      val pos = new Vec2(SInt(FixedPointUtils.width.W))
      val playerPos = new Vec2(SInt(FixedPointUtils.width.W))
      val playerAngle = SInt(FixedPointUtils.width.W)
    }))

    val output = Decoupled(new Bundle {
      val radius = UInt(8.W)
      val visible = Bool()
    })
  })

  io.input.ready := false.B
  io.output.valid := false.B
  io.output.bits := DontCare

  val invSqrt = Module(new InverseSqrt)
  invSqrt.io.result.ready := false.B
  invSqrt.io.input.valid := false.B
  invSqrt.io.input.bits := DontCare
  val triglut = Module(new gameEngine.trig.TrigLUT)

  object S extends ChiselEnum {
    val idle, delta, deltaSquare1, deltaSquare2, computeInvSqrt, normalize1,
        normalize2, calcCos, done =
      Value
  }
  val state = RegInit(S.idle)

  val angleLatch = RegInit(0.S(FixedPointUtils.width.W))
  val playerPosLatch = RegInit(
    Vec2(0.S(FixedPointUtils.width.W), 0.S(FixedPointUtils.width.W))
  )
  val posLatch = RegInit(
    Vec2(0.S(FixedPointUtils.width.W), 0.S(FixedPointUtils.width.W))
  )

  triglut.io.angle := angleLatch
  val deltaInvSqrt = RegInit(0.S(FixedPointUtils.width.W))
  val deltaNorm = RegInit(
    Vec2(0.S(FixedPointUtils.width.W), 0.S(FixedPointUtils.width.W))
  )
  val cosAngleReg = RegInit(0.S(FixedPointUtils.width.W))

  val delta = RegInit(
    Vec2(0.S(FixedPointUtils.width.W), 0.S(FixedPointUtils.width.W))
  )

  val delta2 = RegInit(
    Vec2(0.S(FixedPointUtils.width.W), 0.S(FixedPointUtils.width.W))
  )

  switch(state) {
    is(S.idle) {
      io.input.ready := true.B
      when(io.input.fire) {
        angleLatch := io.input.bits.playerAngle
        playerPosLatch := io.input.bits.playerPos
        posLatch := io.input.bits.pos
        state := S.delta
      }
    }
    is(S.delta) {
      // Calculate delta vector
      delta := posLatch - playerPosLatch
      state := S.deltaSquare1
    }
    is(S.deltaSquare1) {
      delta2.x := delta.x.fpMul(delta.x)
      state := S.deltaSquare2
    }
    is(S.deltaSquare2) {
      delta2.y := delta.y.fpMul(delta.y)
      state := S.computeInvSqrt
    }
    is(S.computeInvSqrt) {
      invSqrt.io.input.bits := delta2.x + delta2.y
      invSqrt.io.input.valid := true.B

      when(invSqrt.io.result.valid) {
        deltaInvSqrt := invSqrt.io.result.bits
        invSqrt.io.result.ready := true.B
        state := S.normalize1
      }
    }
    is(S.normalize1) {
      deltaNorm.x := delta.x.fpMul(deltaInvSqrt)
      state := S.normalize2
    }
    is(S.normalize2) {
      deltaNorm.y := delta.y.fpMul(deltaInvSqrt)
      state := S.calcCos
    }
    is(S.calcCos) {
      cosAngleReg :=
        deltaNorm.x.fpMul(triglut.io.cos) + deltaNorm.y.fpMul(triglut.io.sin)
      state := S.done
    }
    is(S.done) {
      val visible = cosAngleReg > FixedPointUtils.toFP(math.cos(fov / 2.0))
      io.output.bits.visible := visible
      io.output.valid := true.B
      when(io.output.ready) {
        state := S.idle
      }
    }
  }
}
