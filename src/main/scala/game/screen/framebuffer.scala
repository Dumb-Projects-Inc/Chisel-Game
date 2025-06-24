package gameEngine.framebuffer

import gameEngine.screen.VGAInterface

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util.{log2Ceil, Counter, switch, is}
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.screen.VGATiming

/** Another way of interacting with the screen
  *
  * This method utilized double buffering. This takes a lot of space, so a color
  * palette is also used to save space
  *
  * A vga output is generated based on the contents of the first buffer. Writers
  * write to the second buffer. When a new frame begins, buffers are only
  * switched if the valid signal is asserted. This stops unfinished frames from
  * ending up on the screen.
  *
  * A newFrame output, which pulses when we are finished writing a frame, is
  * also provided for convinience
  *
  * @param palette
  *   The color palette to use Consists of a list of RGB values. The buffers
  *   store indexes to this list. Writers must know of this color palette
  */
class DualPaletteFrameBuffer(
    palette: Seq[UInt]
) extends Module {
  private val numColors = palette.length
  private val width = 320
  private val height = 240
  val pal = VecInit(palette)

  val io = IO(new Bundle {
    val valid = Input(Bool())
    val wEnable = Input(Bool())
    val x = Input(UInt(log2Ceil(width).W))
    val y = Input(UInt(log2Ceil(height).W))
    val dataIn = Input(UInt(log2Ceil(numColors).W))
    val vga = Output(new VGAInterface)
    val newFrame = Output(Bool())
  })

  val bufferSel = RegInit(false.B)

  val buffer0 = Module(new Buffer(log2Ceil(numColors)))
  buffer0.io.enable := true.B
  buffer0.io.write := false.B
  buffer0.io.dataIn := 0.U
  buffer0.io.x := 0.U
  buffer0.io.y := 0.U

  val buffer1 = Module(new Buffer(log2Ceil(numColors)))
  buffer1.io.enable := true.B
  buffer1.io.write := false.B
  buffer1.io.dataIn := 0.U
  buffer1.io.x := 0.U
  buffer1.io.y := 0.U

  val vga = Module(new VGATiming)
  io.vga.hsync := vga.io.hSync
  io.vga.vsync := vga.io.vSync

  val pixelIdx = WireDefault(0.U(log2Ceil(numColors).W))
  val color = pal(pixelIdx)
  io.vga.red := Mux(vga.io.visible, color(11, 8), 0.U(4.W))
  io.vga.green := Mux(vga.io.visible, color(7, 4), 0.U(4.W))
  io.vga.blue := Mux(vga.io.visible, color(3, 0), 0.U(4.W))

  val newFrame = vga.io.vSync & !RegNext(vga.io.vSync)
  when(newFrame & io.valid) {
    bufferSel := ~bufferSel
  }

  io.newFrame := newFrame

  when(bufferSel) {
    buffer0.io.x := vga.io.pixelX / 2.U
    buffer0.io.y := vga.io.pixelY / 2.U
    pixelIdx := buffer0.io.dataOut
  }.otherwise {
    buffer1.io.x := vga.io.pixelX / 2.U
    buffer1.io.y := vga.io.pixelY / 2.U
    pixelIdx := buffer1.io.dataOut
  }

  when(io.wEnable) {
    when(bufferSel) {
      buffer1.io.write := true.B
      buffer1.io.x := io.x
      buffer1.io.y := io.y
      buffer1.io.dataIn := io.dataIn
    }.otherwise {
      buffer0.io.write := true.B
      buffer0.io.x := io.x
      buffer0.io.y := io.y
      buffer0.io.dataIn := io.dataIn
    }
  }
}

class Buffer(
    elementWidth: Int,
    memoryFile: Option[String] = None
) extends Module {
  private val width = 320
  private val height = 240
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val x = Input(UInt(log2Ceil(width).W))
    val y = Input(UInt(log2Ceil(height).W))
    val dataIn = Input(UInt(elementWidth.W))
    val dataOut = Output(UInt(elementWidth.W))
  })

  val addr = io.y * width.U + io.x

  if (memoryFile.isDefined) {
    val m = Module(
      new gameEngine.util.RamInitSpWf(
        elementWidth,
        log2Ceil(width * height),
        memoryFile.get
      )
    )
    m.io.clk := clock
    m.io.en := io.enable
    m.io.we := io.write
    m.io.addr := addr
    m.io.di := io.dataIn
    io.dataOut := m.io.dout

  } else {
    val m = Module(
      new gameEngine.util.RamSpWf(elementWidth, log2Ceil(width * height))
    )
    m.io.clk := clock
    m.io.en := io.enable
    m.io.we := io.write
    m.io.addr := addr
    m.io.di := io.dataIn
    io.dataOut := m.io.dout
  }

}

// $COVERAGE-OFF$
private class TopDemo extends Module {
  val io = IO(new Bundle {
    val vga = Output(new VGAInterface)
    val valid = Input(Bool())
  })

  val palette = Seq(
    "hF00".U(12.W), // red
    "h0F0".U(12.W), // green
    "h00F".U(12.W), // blue
    "hFFF".U(12.W) // white
  )

  val fb = Module(new DualPaletteFrameBuffer(palette))
  io.vga := fb.io.vga

  object S extends ChiselEnum {
    val filling, waiting = Value
  }

  val state = RegInit(S.filling)

  val (x, xWrap) = Counter(state === S.filling, 320)
  val (y, yWrap) = Counter(xWrap, 240)

  val (count, wrap) = Counter(true.B, 100000000)
  val (nextColor, nextColorWrap) =
    Counter(wrap & (state === S.waiting), palette.length)

  fb.io.valid := io.valid
  fb.io.wEnable := false.B
  fb.io.x := 0.U
  fb.io.y := 0.U
  fb.io.dataIn := 0.U

  switch(state) {
    is(S.filling) {
      fb.io.wEnable := true.B
      fb.io.x := x
      fb.io.y := y
      fb.io.dataIn := nextColor

      when(xWrap & yWrap) {
        state := S.waiting
      }

    }

    is(S.waiting) {
      when(fb.io.newFrame) {
        state := S.filling
      }
    }
  }

}
// $COVERAGE-ON$
//object bufferMain {
//  def main(args: Array[String]): Unit = {
//    (new ChiselStage).execute(
//      Array(
//        "--target",
//        "systemverilog",
//        "--target-dir",
//        "generated",
//        "--split-verilog"
//      ),
//      Seq(
//        ChiselGeneratorAnnotation(() => new TopDemo),
//        FirtoolOption("--disable-all-randomization")
//      )
//    )
//
//  }
//}
