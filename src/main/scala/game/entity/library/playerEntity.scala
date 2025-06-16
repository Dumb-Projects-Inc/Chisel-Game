package gameEngine.entity

import chisel3._
import chisel3.util._

import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._
import gameEngine.fixed.FixedPointUtils.{toFP, RichFP}
import gameEngine.fixed.FixedPointUtils
import gameEngine.trig.TrigLUT

object PlayerAction extends ChiselEnum {
  val idle, moveForward, moveBackward, strafeLeft, strafeRight, turn = Value
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
    // val health = Input(UInt(8.W)) // Health of the player

    val action = Input(PlayerAction()) // Action input (e.g., move, rotate)
    val actionArg =
      Input(
        SInt(FixedPointUtils.width.W)
      ) // Argument for the action (e.g., speed, angle delta)

  })

  val posReg = RegInit(Vec2(toFP(init_x), toFP(init_y)))
  val angleReg = RegInit(toFP(init_angle))
  // TODO: allow for maps with no walls
  val wallCoordinates = VecInit(
    map.zipWithIndex.flatMap { case (row, y) =>
      row.zipWithIndex.collect { case (1, x) =>
        Vec2(x.U(8.W), y.U(8.W))
      }
    }
  )

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
      wallCoordinates.exists(wall => wall.x === posInt.x && wall.y === posInt.y)
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

  // Pipeline for trigLUT
  val actionReg = RegNext(io.action, PlayerAction.idle)
  val actionArgReg = Reg(SInt(FixedPointUtils.width.W))
  actionArgReg := io.actionArg

  switch(actionReg) {
    is(PlayerAction.idle) {
      // Do nothing, player is idle
    }
    is(PlayerAction.moveForward) {
      val speed = actionArgReg
      val moveDelta = Vec2(
        triglut.io.cos.fpMul(speed),
        triglut.io.sin.fpMul(speed)
      )
      move(moveDelta)
    }
    is(PlayerAction.moveBackward) {
      val speed = io.actionArg
      val moveDelta = Vec2(
        -triglut.io.cos.fpMul(speed),
        -triglut.io.sin.fpMul(speed)
      )
      move(moveDelta)
    }
    is(PlayerAction.strafeLeft) {
      val speed = io.actionArg
      val moveDelta = Vec2(
        triglut.io.cos.fpMul(speed),
        -triglut.io.sin.fpMul(speed)
      )
      move(moveDelta)
    }
    is(PlayerAction.strafeRight) {
      val speed = io.actionArg
      val moveDelta = Vec2(
        -triglut.io.cos.fpMul(speed),
        triglut.io.sin.fpMul(speed)
      )
      move(moveDelta)
    }
    is(PlayerAction.turn) {
      rotate(actionArgReg)
    }
  }
  io.pos := posReg
  io.angle := angleReg
}
