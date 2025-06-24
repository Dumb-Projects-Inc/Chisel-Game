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

class Engine(playerX: Double, playerY: Double) extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val lookRight = Input(Bool())
    val lookLeft = Input(Bool())
    val moveForward = Input(Bool())
    val moveBackward = Input(Bool())
    val txd = Output(Bool())
    val rxd = Input(Bool())
  })

  val scene = Module(new Scene(playerX, playerY))
  scene.io.rxd := io.rxd
  io.txd := scene.io.txd
  io.vga := scene.io.vga

  val (tickCnt, tick) = Counter(true.B, 500000) // 2000000 / 4 = 500000
  scene.io.playerAction := PlayerAction.idle

  val (index, wrap) = Counter(tick, 4)
  val action = VecInit(
    Mux(io.lookLeft, PlayerAction.turnLeft, PlayerAction.idle),
    Mux(io.lookRight, PlayerAction.turnRight, PlayerAction.idle),
    Mux(io.moveForward, PlayerAction.moveForward, PlayerAction.idle),
    Mux(io.moveBackward, PlayerAction.moveBackward, PlayerAction.idle)
  )
  scene.io.playerAction := action(index)

}

class TopModule(playerX: Double, playerY: Double) extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val lookRight = Input(Bool())
    val lookLeft = Input(Bool())
    val moveForward = Input(Bool())
    val moveBackward = Input(Bool())
    val txd = Output(Bool())
    val rxd = Input(Bool())
  })

  val clk50MHz = Wire(Clock())
  val locked = Wire(Bool())

  val resestSync = RegNext(RegNext(RegNext(reset)))

  // $COVERAGE-OFF$
  val pll = Module(new PLLBlackBox)
  pll.io.clock := clock
  pll.io.reset := resestSync
  clk50MHz := pll.io.clk50MHz
  locked := pll.io.locked
  // $COVERAGE-ON$

  withClockAndReset(clk50MHz, !locked) {
    val engine = Module(new Engine(playerX, playerY))
    io.vga := engine.io.vga
    engine.io.lookLeft := io.lookLeft
    engine.io.lookRight := io.lookRight
    engine.io.moveForward := io.moveForward
    engine.io.moveBackward := io.moveBackward
    engine.io.rxd := io.rxd
    io.txd := engine.io.txd
  }
}

import scala.io.StdIn

object gameEngineMain {
  def main(args: Array[String]): Unit = {
    // Parse start positions from args or prompt the user
    val (startX, startY) = if (args.length >= 2) {
      try {
        (args(0).toDouble, args(1).toDouble)
      } catch {
        case _: NumberFormatException =>
          sys.error(
            "Invalid start positions. Please provide two numbers: <startX> <startY>"
          )
      }
    } else {
      print("Enter start X coordinate: ")
      val x = StdIn.readLine().trim.toDouble
      print("Enter start Y coordinate: ")
      val y = StdIn.readLine().trim.toDouble
      (x, y)
    }

    // ChiselStage invocation
    val stageArgs = Array(
      "--target",
      "systemverilog",
      "--target-dir",
      "generated",
      "--split-verilog"
    )

    (new ChiselStage).execute(
      stageArgs,
      Seq(
        ChiselGeneratorAnnotation(() => new TopModule(startX, startY)),
        FirtoolOption("--disable-all-randomization")
      )
    )
  }
}
