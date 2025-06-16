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

import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.pll.PLLBlackBox
import gameEngine.entity.library.WallEntity
import gameEngine.raycast.RaycastDriver
import gameEngine.fixed.InverseSqrt
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.raycast.RayHit

class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val lookRight = Input(Bool())
    val lookLeft = Input(Bool())
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

  val map = Seq(
    Seq(1, 1, 1, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 1, 1)
  )

  val (tickCnt, tick) = Counter(true.B, 2000000)
  val playerAngle = RegInit(toFP(math.Pi / 4))
  when(tick) {
    when(io.lookLeft) {
      playerAngle := playerAngle + toFP(0.01)
    }

    when(io.lookRight) {
      playerAngle := playerAngle - toFP(0.01)
    }
  }

  val heights = RegInit(
    VecInit(Seq.fill(320)(0.U(16.W)))
  )

  val buf = Module(new DualPaletteFrameBuffer(doomPalette))

  val wall = Module(new WallEntity(doomPalette.length, 8, 320))
  wall.io.heights := heights

  val rc = Module(new RaycastDriver(fov = 2, nRays = 320, map = map))
  val distQ = Module(new Queue(SInt(24.W), 4))
  val invsqrt = Module(new InverseSqrt)

  distQ.io.enq.valid := rc.io.response.valid
  distQ.io.enq.bits := rc.io.response.bits.dist
  rc.io.response.ready := distQ.io.enq.ready

  distQ.io.deq <> invsqrt.io.input

  object S extends ChiselEnum {
    val idle, calculate, filling, waiting = Value
  }

  val state = RegInit(S.idle)

  val (x, xWrap) = Counter(state === S.filling, 320)
  val (y, yWrap) = Counter(xWrap && (state === S.filling), 240)

  io.vga := buf.io.vga

  buf.io.x := DontCare
  buf.io.y := DontCare
  buf.io.dataIn := DontCare
  buf.io.wEnable := false.B
  buf.io.valid := false.B
  wall.io.x := DontCare
  wall.io.y := DontCare

  val idx = RegInit(0.U(16.W))

  invsqrt.io.result.ready := false.B
  rc.io.request.valid := false.B
  rc.io.request.bits.angle := DontCare
  rc.io.request.bits.start := DontCare
  switch(state) {
    is(S.idle) {
      rc.io.request.valid := true.B
      rc.io.request.bits.start := Vec2(toFP(1.5), toFP(1.5))
      rc.io.request.bits.angle := playerAngle
      when(rc.io.request.ready) {
        idx := 0.U
        state := S.calculate
      }
    }
    is(S.calculate) {

      invsqrt.io.result.ready := true.B
      when(invsqrt.io.result.valid) {
        heights(idx) := invsqrt.io.result.bits.fpMul(toFP(128.0))(23, 12)
        idx := idx + 1.U

        when(idx === 319.U) {
          state := S.filling
        }
      }

    }
    is(S.filling) {
      buf.io.x := x
      buf.io.y := y
      buf.io.wEnable := true.B

      wall.io.x := x
      wall.io.y := y

      buf.io.dataIn := Mux(wall.io.visible, wall.io.color, 0.U)

      when(xWrap && yWrap) { state := S.waiting }
    }
    is(S.waiting) {
      buf.io.valid := true.B
      when(buf.io.newFrame) {
        state := S.idle
      }
    }
  }

}

class TopModule( /*game input when io works*/ ) extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
    val lookLeft, lookRight = Input(Bool())
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
