package gameEngine.entity

import chisel3._

import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._
import gameEngine.fixed.FixedPointUtils.toFP

class PlayerEntity(init_x: Double, init_y: Double, init_angle: Double)
    extends Module {
  val io = IO(new Bundle {
    // val map = Input() //Map as input for colission detection
    val pos = Output(new Vec2(SInt(24.W)))
    val angle = Output(SInt(32.W)) // Angle of the player
    // val health = Input(UInt(8.W)) // Health of the player

  })
  val posReg = RegInit(Vec2(toFP(init_x), toFP(init_y)))
  val angleReg = RegInit(toFP(init_angle))

  def move(delta: Vec2[SInt]): Unit = {
    posReg := posReg + delta
    // Check collision with map here

  }

  def rotate(angleDelta: SInt): Unit = {
    angleReg := angleReg + angleDelta
    // Normalize angle to [0, 2π)

  }

}
