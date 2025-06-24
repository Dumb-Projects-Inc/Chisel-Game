package gameEngine.demos

import chisel3._
import chisel3.util.{log2Ceil, Counter, switch, is, Enum, PriorityMux}
import chisel3.util.PriorityMux
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}
import gameEngine.pll.PLLBlackBox
import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.vec2.Vec2._
import gameEngine.vec2.Vec2
import gameEngine.entity.library.BobEntity

class WallDemo extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface

    /*     val moveUp = Input(Bool())
    val moveDown = Input(Bool())
    val moveRight = Input(Bool())
    val moveLeft = Input(Bool()) */
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

  val buf = Module(new DualPaletteFrameBuffer(doomPalette))
  io.vga := buf.io.vga

  val bob = Module(
    new BobEntity(
      coordWidth = 16,
      palette = doomPalette,
      period = 25_000_000
    )
  )
  bob.io.scale := 100.U
  bob.io.screen.x := DontCare
  bob.io.screen.y := DontCare
  bob.io.setPos.wrEn := false.B
  bob.io.setPos.x := 1.U
  bob.io.setPos.y := 1.U
  /*   bob.io.moveUp := io.moveUp
  bob.io.moveDown := io.moveDown
  bob.io.moveLeft := io.moveLeft
  bob.io.moveRight := io.moveRight */

  val scale = RegInit(50.U(12.W))
  val bobX = RegInit(160.U(10.W))

  val filling :: waiting :: Nil = Enum(2)
  val state = RegInit(filling)
  val (x, xWrap) = Counter(state === filling, 320)
  val (y, yWrap) = Counter(xWrap && (state === filling), 240)

  val (_, gameTick) = Counter(true.B, 2000000)
  /*   when(gameTick && io.moveDown && scale < 1000.U) {
    scale := scale + 5.U
  }
  when(gameTick && io.moveUp && scale > 5.U) {
    scale := scale - 5.U
  }
  when(gameTick && io.moveRight && scale < 319.U) {
    bobX := bobX + 1.U
  }
  when(gameTick && io.moveLeft && scale > 0.U) {
    bobX := bobX - 1.U
  } */

  bob.io.scale := scale

  buf.io.valid := false.B
  buf.io.wEnable := false.B
  buf.io.x := 0.U
  buf.io.y := 0.U
  buf.io.dataIn := 0.U

  switch(state) {
    is(filling) {
      buf.io.x := x
      buf.io.y := y
      buf.io.wEnable := true.B

      bob.io.screen.x := x
      bob.io.screen.y := y
      bob.io.setPos.wrEn := true.B
      bob.io.setPos.x := bobX
      bob.io.setPos.y := 120.U

      buf.io.dataIn := PriorityMux(
        Seq(
          bob.io.visible -> bob.io.pixel,
          true.B -> 3.U // Just a different background
        )
      )

      when(xWrap && yWrap) { state := waiting }
    }
    is(waiting) {
      buf.io.valid := true.B
      when(buf.io.newFrame) {
        state := filling
      }
    }
  }
}

class WallDemoTop extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface

    val moveUp = Input(Bool())
    val moveDown = Input(Bool())
    val moveRight = Input(Bool())
    val moveLeft = Input(Bool())
  })

  val clk50MHz = Wire(Clock())
  val locked = Wire(Bool())

  val pll = Module(new PLLBlackBox)
  pll.io.clock := clock
  pll.io.reset := reset
  clk50MHz := pll.io.clk50MHz
  locked := pll.io.locked

  withClockAndReset(clk50MHz, !locked) {
    val demo = Module(new WallDemo)
    /*     demo.io.moveUp := io.moveUp
    demo.io.moveDown := io.moveDown
    demo.io.moveRight := io.moveRight
    demo.io.moveLeft := io.moveLeft */
    io.vga := demo.io.vga
  }
}

object WallDemoMain {
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
        ChiselGeneratorAnnotation(() => new WallDemoTop),
        FirtoolOption("--disable-all-randomization")
      )
    )
  }
}
