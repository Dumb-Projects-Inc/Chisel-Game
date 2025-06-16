package gameEngine.raycast

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2._
import gameEngine.fixed.InverseSqrt
import gameEngine.entity.library.WallEntity
import gameEngine.framebuffer.DualPaletteFrameBuffer

class scene3D extends Module {
  val io = IO(new Bundle {
    val out = Output(Vec(320, UInt(16.W)))
  })

  val heights = RegInit(
    VecInit(Seq.fill(320)(0.U(16.W)))
  )
  io.out := heights

  val rc = Module(new RaycastDriver)
  val invsqrt = Module(new InverseSqrt)

  rc.io.request.valid := true.B
  rc.io.request.bits.start := Vec2(toFP(1.5), toFP(1.5))
  rc.io.request.bits.angle := toFP(math.Pi / 4)

  invsqrt.io.input.bits := rc.io.response.bits.dist
  invsqrt.io.input.valid := rc.io.response.valid
  rc.io.response.ready := invsqrt.io.input.ready

  val (idx, wrap) = Counter(invsqrt.io.result.valid, 320)

  invsqrt.io.result.ready := true.B
  when(invsqrt.io.result.valid) {
    heights(idx) := invsqrt.io.result.bits.fpMul(toFP(128.0))(23, 12)
  }

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

  val wall = Module(new WallEntity(doomPalette.length, 8, 320))
  val buf = Module(new DualPaletteFrameBuffer(doomPalette))

}

class scene3dSpec extends AnyFunSpec with ChiselSim with Matchers {

  describe("TEST") {
    it("pass general test cases") {
      simulate(new scene3D) { dut =>
        dut.clock.step(100)
      }
    }
  }
}
