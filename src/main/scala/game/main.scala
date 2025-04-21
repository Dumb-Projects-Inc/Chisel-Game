package gameEngine

import chisel3._
import chisel3.util.log2Ceil
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}

import gameEngine.util._

class Vec2(precision: Int = 10) extends Bundle {
  val x = UInt(precision.W)
  val y = UInt(precision.W)
}

class FixedPointDecimal(val totalBits: Int, val fracBits: Int) extends Bundle {
  val raw = SInt(totalBits.W)

  def +(that: FixedPointDecimal): FixedPointDecimal = {
    require(totalBits == that.totalBits && fracBits == that.fracBits)
    val sum = Wire(new FixedPointDecimal(totalBits, fracBits))
    sum.raw := this.raw + that.raw
    sum
  }

  def -(that: FixedPointDecimal): FixedPointDecimal = {
    require(totalBits == that.totalBits && fracBits == that.fracBits)
    val diff = Wire(new FixedPointDecimal(totalBits, fracBits))
    diff.raw := this.raw - that.raw
    diff
  }

  def *(that: FixedPointDecimal): FixedPointDecimal = {
    require(totalBits == that.totalBits && fracBits == that.fracBits)
    val prod = Wire(new FixedPointDecimal(totalBits, fracBits))
    val extended = (this.raw.asSInt * that.raw.asSInt).asSInt
    prod.raw := (extended >> fracBits).asSInt
    prod
  }
}

class UartEngine(clkSpeed: Int) extends Module {
  val io = IO(new Bundle {
    val tx = Output(Bool())
    val x = Input(new FixedPointDecimal(4, 2))
    val y = Input(new FixedPointDecimal(4, 2))
  })

  val uart = Module(new BufferedTx(clkSpeed, 115200))
  io.tx := uart.io.txd

  val prod = io.x * io.y

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
        ChiselGeneratorAnnotation(() => new UartEngine(100000000)),
        FirtoolOption("--disable-all-randomization")
      )
    )

  }
}
