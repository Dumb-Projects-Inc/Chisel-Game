package gameEngine.entity

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import _root_.gameEngine.entity.PalettedIndexSprite

class PalettedIndexSpriteSpec extends AnyFunSpec with ChiselSim with Matchers {
  describe("PalettedIndexSprite") {
    val filepath = "smiley-64.png"
    val palette = Seq(
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
      "h0F0".U(12.W), // #00FF00
      "h08F".U(12.W) // #0088FF
    )
    val maxW = 128
    val maxH = 128

    it("should match known pixels at scale = 100") {
      simulate(
        new PalettedIndexSprite(filepath, 64, 64, palette, maxW, maxH)
      ) { dut =>
        val cases = Seq(
            // Testing if a point (x,y) has the correct value from the pallette and or is visable.
            // Format is (x, y, palletteIdx, transparent)
          (0, 0, 0, true), // top left corner
          (32, 32, 12, false), // center of face
          (63, 63, 0, true), // center of face
        )

        for ((x, y, expIdx, expT) <- cases) {
            dut.io.x.poke(x.U); dut.io.y.poke(y.U); dut.io.scale.poke(100.U)
            dut.clock.step(3)
            dut.io.idx.expect(expIdx.U)
            dut.io.transparent.expect(expT.B)
        }
      }
    }
    it("should match known pixels at scale = 50") {
      simulate(
        new PalettedIndexSprite(filepath, 64, 64, palette, maxW, maxH)
      ) { dut =>
        val cases = Seq(
          (0, 0, 0, true),
          (16, 16, 12, false),
          (31, 31, 0, true),
        )

        for ((x, y, expIdx, expT) <- cases) {
            dut.io.x.poke(x.U); dut.io.y.poke(y.U); dut.io.scale.poke(50.U)
            dut.clock.step(3)
            dut.io.idx.expect(expIdx.U)
            dut.io.transparent.expect(expT.B)
        }
      }
    }
    it("should match known pixels at scale = 200") {
      simulate(
        new PalettedIndexSprite(filepath, 64, 64, palette, maxW, maxH)
      ) { dut =>
        val cases = Seq(
          (0, 0, 0, true),
          (64, 64, 12, false),
          (127, 127, 0, true),
        )

        for ((x, y, expIdx, expT) <- cases) {
            dut.io.x.poke(x.U); dut.io.y.poke(y.U); dut.io.scale.poke(200.U)
            dut.clock.step(3)
            dut.io.idx.expect(expIdx.U)
            dut.io.transparent.expect(expT.B)
        }
      }
    }
    it("should match known pixels at scale = 122") {
      simulate(
        new PalettedIndexSprite(filepath, 64, 64, palette, maxW, maxH)
      ) { dut =>
        val cases = Seq(
          (0, 0, 0, true),
          (39, 39, 12, false),
          (77, 77, 0, true),
        )

        for ((x, y, expIdx, expT) <- cases) {
            dut.io.x.poke(x.U); dut.io.y.poke(y.U); dut.io.scale.poke(122.U)
            dut.clock.step(3)
            dut.io.idx.expect(expIdx.U)
            dut.io.transparent.expect(expT.B)
        }
      }
    }
  }
}
