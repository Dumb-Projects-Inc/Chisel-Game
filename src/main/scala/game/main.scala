package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.renderer.RainbowRenderer
import gameEngine.sprite.ImageSprite

class Vec2t[T <: Data](gen: T) extends Bundle {
  val x, y = gen.cloneType
}

object Vec2t {
  def apply[T <: Data](x: T, y: T): Vec2t[T] = {
    val w = Wire(new Vec2t(x.cloneType))
    w.x := x
    w.y := y
    w
  }
}

class Engine extends Module {
  val io = IO(new Bundle {
    val x, y = Input(UInt(8.W))
    val a = Output(new Vec2t(UInt(12.W)))
    val b = Output(new Vec2t(SInt(12.W)))
  })

  val vec = Vec2t(io.x - 5.U, io.y - 2.U)

  io.a := vec
  io.b := Vec2t((io.x + io.x).asSInt, (io.y + io.y).asSInt)

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
