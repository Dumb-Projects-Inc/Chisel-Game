package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.renderer.RainbowRenderer
import gameEngine.entity.SpriteEntity
import gameEngine.entity.library.SmileyEntity

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val bob = Module(new SmileyEntity(10))
  val alice = Module(new SmileyEntity(10))
  val crazyBob = Module(new SmileyEntity(10))
  val controller = Module(new VGAController)
  val rainbow = Module(new RainbowRenderer)

  bob.io.screen.x := controller.io.x
  bob.io.screen.y := controller.io.y
  alice.io.screen.x := controller.io.x
  alice.io.screen.y := controller.io.y
  crazyBob.io.screen.x := controller.io.x
  crazyBob.io.screen.y := controller.io.y

  bob.io.setPos.wrEn := true.B
  bob.io.setPos.x := 120.U
  bob.io.setPos.y := 80.U

  alice.io.setPos.wrEn := true.B
  alice.io.setPos.x := 200.U
  alice.io.setPos.y := 50.U

  val crazyBobCounter = RegInit(0.U(26.W))
  val newBobX = RegInit(100.U(10.W))
  val newBobY = RegInit(100.U(10.W))

  crazyBob.io.setPos.wrEn := true.B
  crazyBobCounter := crazyBobCounter + 1.U
  when(crazyBobCounter === (1_000_000 - 1).U) {
    crazyBobCounter := 0.U
    newBobX := Mux(newBobX === 0.U, 100.U, newBobX - 1.U)
    newBobY := Mux(newBobY === 0.U, 100.U, newBobY - 1.U)
  }
  crazyBob.io.setPos.x := newBobX
  crazyBob.io.setPos.y := newBobY

  rainbow.io.x := controller.io.x
  rainbow.io.y := controller.io.y

  val visVec = VecInit(
    Seq(bob.io.visible, alice.io.visible, crazyBob.io.visible)
  )
  val pixVec = VecInit(Seq(bob.io.pixel, alice.io.pixel, crazyBob.io.pixel))

  val chosenPixel = PriorityMux(visVec.zip(pixVec))
  val anyVisible = visVec.asUInt.orR

  controller.io.pixel := Mux(anyVisible, chosenPixel, rainbow.io.resultPixel)

  io.vga := controller.io.vga

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
        ChiselGeneratorAnnotation(() => new Engine()),
        FirtoolOption("--disable-all-randomization")
      )
    )

  }
}
