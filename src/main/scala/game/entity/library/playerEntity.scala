package gameEngine.entity

import chisel3._
import chisel3.util.{switch, is, Decoupled}

import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._
import gameEngine.fixed.FixedPointUtils.{toFP, RichFP}
import gameEngine.fixed.FixedPointUtils
import gameEngine.trig.TrigLUT

object PlayerAction extends ChiselEnum {
  val idle, moveForward, moveBackward, strafeLeft, strafeRight, turnLeft,
      turnRight, turn = Value
}

class PlayerEntity(
    init_x: Double,
    init_y: Double,
    init_angle: Double,
    map: Seq[Seq[Int]] = Seq.empty
) extends Module {
  val io = IO(new Bundle {
    // val map = Input() //Map as input for colission detection
    val pos = Output(new Vec2(SInt(24.W)))
    val angle = Output(SInt(32.W)) // Angle of the player
    // val health = Input(UInt(8.W))

    val action =
      Flipped(Decoupled(PlayerAction())) // Action input (e.g., move, rotate)
    val actionArg =
      Input(
        SInt(FixedPointUtils.width.W)
      ) // Argument for the action (e.g., speed, angle delta)

  })

  val posReg = RegInit(Vec2(toFP(init_x), toFP(init_y)))
  val angleReg = RegInit(toFP(init_angle))
  val mapVec = VecInit.tabulate(map.length, map(0).length) { (x, y) =>
    map(x)(y).B
  }

  val triglut = Module(new TrigLUT)
  triglut.io.angle := angleReg

  def move(delta: Vec2[SInt]): Unit = {
    val newPos = posReg + delta
    // Check collision with map here
    val posInt = Vec2(
      newPos.x(23, 12),
      newPos.y(23, 12)
    )

    // check if posInt is within wallCoordinates
    when(
      // wallCoordinates.exists(wall => wall.x === posInt.x && wall.y === posInt.y)
      mapVec(posInt.y)(posInt.x) === 1.B
    ) {
      // Collision detected, do not update position
    }.otherwise {
      posReg := newPos
    }
  }

  def rotate(angleDelta: SInt): Unit = {
    val newAngle = angleReg + angleDelta
    // Normalize angle to [0, 2π)
    when(newAngle < toFP(0)) {
      angleReg := newAngle + toFP(2 * math.Pi)
    }.elsewhen(newAngle >= toFP(2 * math.Pi)) {
      angleReg := newAngle - toFP(2 * math.Pi)
    }.otherwise {
      angleReg := newAngle
    }

  }

  object S extends ChiselEnum {
    val idle, computeX, computeY, move = Value
  }
  val state = RegInit(S.idle)

  val latchedAction = RegInit(PlayerAction.idle)
  val latchedActionArg = RegInit(0.S(FixedPointUtils.width.W))

  io.action.ready := state === S.idle // Only accept new action when idle

  when(io.action.fire) {
    // Latch for multi clock cycle actions
    latchedAction := io.action.bits
    latchedActionArg := io.actionArg
    switch(io.action.bits) {
      is(PlayerAction.turnLeft, PlayerAction.turnRight, PlayerAction.turn) {
        rotate(io.actionArg)
      }
      is(
        PlayerAction.moveForward,
        PlayerAction.moveBackward,
        PlayerAction.strafeLeft,
        PlayerAction.strafeRight
      ) {
        state := S.computeX
      }
      is(PlayerAction.idle) {
        // No action needed for idle
      }
    }

  }

  val delta = Reg(new Vec2(SInt(FixedPointUtils.width.W)))

  switch(state) {
    is(S.idle) {}
    is(S.computeX) {
      switch(latchedAction) {
        is(PlayerAction.moveForward) {
          delta.x := triglut.io.cos.fpMul(latchedActionArg)
        }
        is(PlayerAction.moveBackward) {
          delta.x := -triglut.io.cos.fpMul(latchedActionArg)
        }
        is(PlayerAction.strafeLeft) {
          delta.x := triglut.io.sin.fpMul(latchedActionArg)
        }
        is(PlayerAction.strafeRight) {
          delta.x := -triglut.io.sin.fpMul(latchedActionArg)
        }
      }
      state := S.computeY // go back to fetch next command
    }
    is(S.computeY) {
      switch(latchedAction) {
        is(PlayerAction.moveForward) {
          delta.y := triglut.io.sin.fpMul(latchedActionArg)
        }
        is(PlayerAction.moveBackward) {
          delta.y := -triglut.io.sin.fpMul(latchedActionArg)
        }
        is(PlayerAction.strafeLeft) {
          delta.y := -triglut.io.cos.fpMul(latchedActionArg)
        }
        is(PlayerAction.strafeRight) {
          delta.y := triglut.io.cos.fpMul(latchedActionArg)
        }
      }
      state := S.move // go back to fetch next command
    }
    is(S.move) {
      // Move the player
      move(delta)
      state := S.idle // Go back to idle state after moving
    }
  }

  io.pos := posReg
  io.angle := angleReg
}
