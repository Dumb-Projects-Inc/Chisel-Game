package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen._
import gameEngine.vec2.Vec2
import gameEngine.fixed.FixedPointUtils._
import gameEngine.trig.TrigLUT
import chisel3.util.MuxCase
import gameEngine.screen.raycast.Raycaster

class Engine extends Module {

  val pos = Vec2(toFP(1.0), toFP(1.0))
  val angle = toFP(math.Pi / 2)

  val trig = Module(new TrigLUT)
  trig.io.angle := angle

  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  val mapSeq = Seq(
    Seq(1, 1, 1),
    Seq(1, 0, 1),
    Seq(1, 1, 1)
  )
  val map = VecInit.tabulate(3, 3) { (x, y) => mapSeq(x)(y).B }

  val raycaster = Module(new Raycaster)
  raycaster.io.in.bits.start := pos
  raycaster.io.in.bits.angle := angle
  raycaster.io.in.valid := true.B
  raycaster.io.out.ready := false.B

  val x = raycaster.io.out.bits.pos.x
  val y = raycaster.io.out.bits.pos.y

  val idx = Mux(
    raycaster.io.out.bits.isHorizontal,
    MuxCase(
      Vec2(0.S, 0.S),
      Seq(
        (east & north) -> Vec2(x.fpFloor, y.fpFloor)
      )
    ),
    MuxCase(
      Vec2(0.S, 0.S),
      Seq(
        (east & north) -> Vec2(x.fpFloor, y.fpFloor)
      )
    )
  )

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
