package gameEngine.entity

import chisel3._
import chisel3.util.{Enum, switch, is}
import chisel3.util.switch
import gameEngine.fixed.FixedPointUtils.{toFP, RichFP}
import gameEngine.fixed.FixedPointUtils

class Scene extends Module {
  val io = IO(new Bundle {
    val playerAction = Input(PlayerAction())
  })

  val _map = Seq(
    Seq(1, 1, 1, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 1, 1)
  )

  val map = VecInit.tabulate(4, 4) { (x, y) => _map(x)(y).B }
  val player = Module(new PlayerEntity(0.0, 0.0, 0.0, _map))

  // Entity management
  object S extends ChiselEnum {
    val idle, updatePlayer, render, draw = Value
  }
  val state = RegInit(S.idle)

  val actionPending = RegInit(PlayerAction.idle)
  val hasPendingAction = RegInit(false.B)

  // Player action handling
  when(io.playerAction =/= PlayerAction.idle && !hasPendingAction) {
    actionPending := io.playerAction
    hasPendingAction := true.B
  }

  player.io.action := Mux(
    state === S.updatePlayer,
    actionPending,
    PlayerAction.idle
  )
  val defaultArg = Wire(SInt(FixedPointUtils.width.W))
  defaultArg := 0.S
  val moveSpeed = 1.0 // Default move speed
  switch(actionPending) {
    is(PlayerAction.moveForward) { defaultArg := toFP(moveSpeed) }
    is(PlayerAction.moveBackward) { defaultArg := toFP(moveSpeed) }
    is(PlayerAction.strafeLeft) { defaultArg := toFP(moveSpeed) }
    is(PlayerAction.strafeRight) { defaultArg := toFP(moveSpeed) }
    is(PlayerAction.turn) { defaultArg := toFP(0.2) } // e.g. turn by 0.2 rad
  }
  player.io.actionArg := Mux(state === S.updatePlayer, defaultArg, 0.S)

  switch(state) {
    is(S.idle) {
      // Wait for input or trigger to start updating
      // Transition to updatePlayer or updateEnemies based on input
      state := S.updatePlayer
    }
    is(S.updatePlayer) {
      hasPendingAction := false.B
      state := S.render
    }

    is(S.render) {
      // Prepare rendering data
      state := S.draw
    }
    is(S.draw) {
      // Draw the scene to the framebuffer
      state := S.idle
    }
  }

}
