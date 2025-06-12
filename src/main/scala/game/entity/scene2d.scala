package gameEngine.entity

import chisel3._
import chisel3.util.{Enum, switch, is}
import chisel3.util.switch
import os.stat

class Scene2D extends Module {
  val io = IO(new Bundle {})

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
  val player = Module(new PlayerEntity(0.0, 0.0, 0.0))

  // Entity management
  val idle :: updatePlayer :: updateEnemies :: render :: wallheight :: draw :: Nil =
    Enum(5)
  val state = RegInit(idle)

  switch(state) {
    is(idle) {
      // Wait for input or trigger to start updating
      // Transition to updatePlayer or updateEnemies based on input
      state := updatePlayer
    }
    is(updatePlayer) {
      // Update player position, angle, etc.
      state := updateEnemies
    }
    is(updateEnemies) {
      // Update enemies' positions and states
      state := render
    }
    is(render) {
      // Prepare rendering data
      state := wallheight
    }
    is(wallheight) {
      state := draw
    }
    is(draw) {
      // Draw the scene to the framebuffer
      state := idle
    }
  }

}
