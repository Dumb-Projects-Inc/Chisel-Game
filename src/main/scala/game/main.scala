package gameEngine

import chisel3._
import chisel3.util.{
  log2Ceil,
  Counter,
  switch,
  is,
  Enum,
  PriorityMux,
  Queue,
  Decoupled
}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.entity.Scene
import gameEngine.entity.PlayerAction
import gameEngine.pll.PLLBlackBox
import gameEngine.screen.VGAInterface

import gameEngine.fixed.FixedPointUtils._

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val lookRight = Input(Bool())
    val lookLeft = Input(Bool())
    val moveForward = Input(Bool())
    val moveBackward = Input(Bool())
  })

  val scene = Module(new Scene)
  io.vga := scene.io.vga

  val (tickCnt, tick) = Counter(true.B, 2000000)
  scene.io.playerAction := PlayerAction.idle

  when(tick) {
    when(io.lookLeft) {
      scene.io.playerAction := PlayerAction.turnLeft
    }
    when(io.lookRight) {
      scene.io.playerAction := PlayerAction.turnRight
    }
    when(io.moveForward) {
      scene.io.playerAction := PlayerAction.moveForward
    }
    when(io.moveBackward) {
      scene.io.playerAction := PlayerAction.moveBackward
    }
  }

}

class TopModule( /*game input when io works*/ ) extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val lookRight = Input(Bool())
    val lookLeft = Input(Bool())
    val moveForward = Input(Bool())
    val moveBackward = Input(Bool())
  })

  val clk50MHz = Wire(Clock())
  val locked = Wire(Bool())

  // $COVERAGE-OFF$
  val pll = Module(new PLLBlackBox)
  pll.io.clock := clock
  pll.io.reset := reset
  clk50MHz := pll.io.clk50MHz
  locked := pll.io.locked
  // $COVERAGE-ON$

  withClockAndReset(clk50MHz, !locked) {
    val engine = Module(new Engine)
    io.vga := engine.io.vga
    engine.io.lookLeft := io.lookLeft
    engine.io.lookRight := io.lookRight
    engine.io.moveForward := io.moveForward
    engine.io.moveBackward := io.moveBackward
  }
}

object gameEngineMain {
  def main(args: Array[String]): Unit = {
    // Splitting files is expected, and the blackbox will generate syntax errors if not split
    (new ChiselStage).execute(
      Array(
        "--target",
        "systemverilog",
        "--target-dir",
        "generated",
        "--split-verilog"
      ),
      Seq(
        ChiselGeneratorAnnotation(() => new TopModule),
        FirtoolOption("--disable-all-randomization")
      )
    )

  }
}
