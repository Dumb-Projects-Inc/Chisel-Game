package gameEngine.entity

import chisel3._
import chisel3.util._
import gameEngine.fixed.FixedPointUtils._
import gameEngine.fixed.FixedPointUtils
import gameEngine.fixed.InverseSqrt
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._

class CircleEntity(fov: Double = 1.5, map: Seq[Seq[Int]] = Seq.empty)
    extends Module {
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

  val mapVec = VecInit.tabulate(map.length, map(0).length) { (x, y) =>
    map(x)(y).B
  }

  io.input.ready := false.B
  io.output.valid := false.B
  io.output.bits := DontCare

  val invSqrt = Module(new InverseSqrt)
  invSqrt.io.result.ready := false.B
  invSqrt.io.input.valid := false.B
  invSqrt.io.input.bits := DontCare
  val triglut = Module(new gameEngine.trig.TrigLUT)

  object S extends ChiselEnum {
    val idle, delta, deltaSquare1, deltaSquare2, computeCosNorm, normalize1,
        normalize2, calcCos, stepInit, stepCompute, compueSinNorm, calcXoffset,
        done =
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
  val deltaCosNorm = RegInit(0.S(FixedPointUtils.width.W))
  val deltaSinNorm = RegInit(0.S(FixedPointUtils.width.W))
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

  // Stepping variables
  val steps = 16
  val invSteps = toFP(1.0 / steps.toDouble)

  val curr = Reg(new Vec2(SInt(FixedPointUtils.width.W)))
  val targetTile = Reg(new Vec2(UInt(12.W)))
  val hitWall = RegInit(false.B)
  val stepIdx = RegInit(0.U(log2Ceil(steps).W))
  val step = Reg(new Vec2(SInt(FixedPointUtils.width.W)))

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
      state := S.computeCosNorm
    }
    is(S.computeCosNorm) {
      invSqrt.io.input.bits := delta2.x + delta2.y
      invSqrt.io.input.valid := true.B

      when(invSqrt.io.result.valid) {
        deltaCosNorm := invSqrt.io.result.bits
        invSqrt.io.result.ready := true.B
        state := S.normalize1
      }
    }
    is(S.normalize1) {
      deltaNorm.x := delta.x.fpMul(deltaCosNorm)
      state := S.normalize2
    }
    is(S.normalize2) {
      deltaNorm.y := delta.y.fpMul(deltaCosNorm)
      state := S.calcCos
    }
    is(S.calcCos) {
      cosAngleReg :=
        deltaNorm.x.fpMul(triglut.io.cos) + deltaNorm.y.fpMul(triglut.io.sin)
      state := S.stepInit
    }
    is(S.stepInit) {
      step := Vec2(
        delta.x.fpMul(invSteps),
        delta.y.fpMul(invSteps)
      )
      curr := playerPosLatch
      targetTile := Vec2(posLatch.x(23, 12), posLatch.y(23, 12))
      hitWall := false.B
      stepIdx := 0.U

      state := S.stepCompute
    }
    is(S.stepCompute) {
      when(stepIdx < steps.U && !hitWall) {
        val tile = Vec2(
          curr.x(23, 12),
          curr.y(23, 12)
        )

        when(tile.x === targetTile.x && tile.y === targetTile.y) {
          // Reached sprite tile
          state := S.done
        }.elsewhen(mapVec(tile.y)(tile.x) === 1.B) {
          hitWall := true.B
          state := S.done
        }.otherwise {
          curr := curr + step
          stepIdx := stepIdx + 1.U
        }
      }.otherwise {
        state := S.done
      }
    }
    is(S.done) {
      val visible = cosAngleReg > toFP(math.cos(fov / 2.0))
      io.output.bits.visible := visible && !hitWall
      io.output.valid := true.B
      when(io.output.ready) {
        state := S.idle
      }
    }
  }
}
