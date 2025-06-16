package gameEngine.demos

import chisel3._
import chisel3.util.{log2Ceil, Counter, switch, is, Enum, PriorityMux}
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.entity.library.WallEntity
import gameEngine.vec2.Vec2._
import gameEngine.vec2.Vec2
import gameEngine.entity.library.SmileyEntity

import gameEngine.raycast.{RayRequest, RaycastDriver}
import gameEngine.fixed.FixedPointUtils.toFP

class RaycastWallDemo(
  fov: Double   = 2.0,
  nRays: Int    = 240,
  maxSteps: Int = 32,
  nTiles: Int   = 16
) extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  //50 MHz
  val clock50 = RegInit(false.B)
  clock50 := ~clock50

    val doomPalette = Seq(
    "h000".U(12.W), // #000000
    "h222".U(12.W), // #222222
    "h444".U(12.W), // #444444
    "h777".U(12.W), // #777777
    "hAAA".U(12.W), // #AAAAAA
    "hFFF".U(12.W), // #FFFFFF

    "h430".U(12.W), // #443300
    "h640".U(12.W), // #664400
    "hA50".U(12.W), // #AA5500
    "hD90".U(12.W), // #DD9900

    "hF20".U(12.W), // #FF2200
    "hF60".U(12.W), // #FF6600
    "hFF0".U(12.W), // #FFFF00

    "hA00".U(12.W), // #AA0000
    "hF00".U(12.W), // #FF0000
    "h800".U(12.W) // #880000
  )

  val buf = Module(new DualPaletteFrameBuffer(doomPalette))
  io.vga <> buf.io.vga
  buf.io.valid   := false.B
  buf.io.wEnable := false.B
  buf.io.x       := 0.U
  buf.io.y       := 0.U
  buf.io.dataIn  := 0.U

  val ray = Module(new RaycastDriver(fov, nRays, nTiles))
  val startX     = RegNext(toFP(2.0)) 
  val startY     = RegNext(toFP(2.0))
  val startAngle = RegNext(0.S(24.W))
  ray.io.request.valid  := false.B
  ray.io.response.ready := false.B
  ray.io.request.bits.start.x := startX
  ray.io.request.bits.start.y := startY
  ray.io.request.bits.angle   := startAngle

  val hitDists = RegInit(VecInit(Seq.fill(nRays)(0.S(24.W))))
  val hitTiles = RegInit(VecInit(Seq.fill(nRays)(0.U(log2Ceil(nTiles).W))))
  val sent     = RegInit(false.B)
  val idx      = RegInit(0.U(log2Ceil(nRays+1).W))

  when (buf.io.newFrame) {
    sent := false.B
    idx  := 0.U
  }

  when (clock50) {
    when (!sent) {
      ray.io.request.valid := true.B
      when (ray.io.request.fire) {
        sent := true.B
      }
    }
    .elsewhen (sent && (idx < nRays.U)) {
      ray.io.response.ready := true.B
      when (ray.io.response.valid) {
        val rd = RegNext(ray.io.response.bits.dist)
        val rt = RegNext(ray.io.response.bits.tile)
        hitDists(idx) := rd
        hitTiles(idx) := rt
        idx := idx + 1.U
      }
    }
    .elsewhen (sent && (idx >= nRays.U)) {
      val drawIdxRaw = idx - nRays.U
      val drawIdx    = RegNext(drawIdxRaw)
      val hRaw = ((240.S * toFP(1.0)) / (hitDists(drawIdx))).asUInt
      val hReg = RegNext(hRaw)
      val tileReg = RegNext(hitTiles(drawIdx))

      val validReg   = RegNext(true.B, init=false.B)
      val weReg      = RegNext(true.B, init=false.B)
      val xReg       = RegNext(drawIdx)
      val yReg       = RegNext(hReg(log2Ceil(240)-1,0))
      val dataReg    = RegNext(tileReg)

      buf.io.valid   := validReg
      buf.io.wEnable := weReg
      buf.io.x       := xReg
      buf.io.y       := yReg
      buf.io.dataIn  := dataReg

      idx := idx + 1.U
    }
  }
}

object RaycastWallDemoMain {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).execute(
      Array(
        "--target",
        "systemverilog",
        "--target-dir",
        "generated",
        "--split-verilog"
      ),
      Seq(
        ChiselGeneratorAnnotation(() => new RaycastWallDemo),
        FirtoolOption("--disable-all-randomization")
      )
    )
  }
}
