package gameEngine.demos

import chisel3._
import chisel3.util.{log2Ceil, Counter, switch, is, Enum, PriorityMux}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.entity.library.SquareEntity
import gameEngine.vec2.Vec2._
import gameEngine.vec2.Vec2

class MovingBlocks extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val moveX = Input(Bool())
    val moveY = Input(Bool())
  })

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

  val (gameCount, gameTick) = Counter(true.B, 2000000)

  val pos = RegInit(Vec2(0.U(9.W), 0.U(9.W)))

  when(gameTick) {
    when(io.moveX) {
      pos.x := pos.x + 2.U
    }
    when(io.moveY) {
      pos.y := pos.y + 2.U
    }

    when(io.moveX & io.moveY) {
      pos.x := pos.x + 1.U
      pos.y := pos.y + 1.U
    }

  }

  val buf = Module(new DualPaletteFrameBuffer(doomPalette))
  io.vga := buf.io.vga

  val wall1 = Module(new SquareEntity(doomPalette.length, 9))
  wall1.io.p1 := Vec2(20.U, 20.U)
  wall1.io.p2 := Vec2(120.U, 120.U)
  wall1.io.p := DontCare

  val wall2 = Module(new SquareEntity(doomPalette.length, 15))
  wall2.io.p1 := pos
  wall2.io.p2 := pos + Vec2(50.U, 50.U)
  wall2.io.p := DontCare

  // Connect screen to buffer
  val filling :: waiting :: Nil = Enum(2)
  val state = RegInit(filling)

  val (x, xWrap) = Counter(state === filling, 320)
  val (y, yWrap) = Counter(xWrap, 240)

  buf.io.valid := false.B
  buf.io.wEnable := false.B
  buf.io.x := 0.U
  buf.io.y := 0.U
  buf.io.dataIn := 0.U

  switch(state) {
    is(filling) {
      buf.io.wEnable := true.B
      buf.io.x := x
      buf.io.y := y

      wall1.io.p := Vec2(x, y)
      wall2.io.p := Vec2(x, y)

      buf.io.dataIn := PriorityMux(
        Seq(
          wall2.io.visible -> wall2.io.colorOut,
          wall1.io.visible -> wall1.io.colorOut,
          true.B -> 0.U
        )
      )

      when(xWrap && yWrap) {
        state := waiting
      }
    }

    is(waiting) {
      buf.io.valid := true.B
      when(buf.io.newFrame) {
        state := filling
      }
    }
  }

}
object MovingBlocksMain {
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
        ChiselGeneratorAnnotation(() => new MovingBlocks),
        FirtoolOption("--disable-all-randomization")
      )
    )

  }
}
