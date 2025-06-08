package gameEngine.entity

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import scala.io.Source

class SpriteImageUtilSpec extends AnyFlatSpec with Matchers {

  "SpriteImageUtil.loadPngData" should "correctly load a 3x3 sprite with expected colors" in {
    // test_sprite is is a 3x3 png
    val src = getClass.getResource("/test_sprite.png")
    src should not be null
    val (pixels, width, height) = SpriteImageUtil.loadPngData(src.getPath)

    width shouldEqual 3
    height shouldEqual 3

    // expected pixel values starting with the top column left to right
    val expectedPixels = Seq(
      BigInt("0111100000000", 2),
      BigInt("0000011110000", 2),
      BigInt("0000000001111", 2),
      BigInt("0000000000000", 2),
      BigInt("0111111111111", 2),
      BigInt("0000000000000", 2),
      BigInt("1000000000000", 2),
      BigInt("1000000000000", 2),
      BigInt("1000000000000", 2)
    )

    pixels shouldEqual expectedPixels
  }
}

class ImageSpriteSpec extends AnyFlatSpec {
  behavior of "ImageSprite"
  it should "output correct colors and transparency for each pixel" in {
    val filepath = "src/test/resources/test_sprite.png"
    simulate(new ImageSprite(filepath, 3, 3)) { dut =>
      val expectedPixels = Seq(
        (0, 0, (15, 0, 0), false), // F00
        (1, 0, (0, 15, 0), false), // 3F0
        (2, 0, (0, 0, 15), false), // 04F
        (0, 1, (0, 0, 0), false), // FFF
        (1, 1, (15, 15, 15), false), // FFF
        (2, 1, (0, 0, 0), false), // FFF
        (0, 2, (0, 0, 0), true), // 000 => transparent
        (1, 2, (0, 0, 0), true), // 000 => transparent
        (2, 2, (0, 0, 0), true) // 000 => transparent
      )

      for ((x, y, (expR, expG, expB), expTransp) <- expectedPixels) {
        dut.io.x.poke(x.U)
        dut.io.y.poke(y.U)
        dut.clock.step()
        dut.io.r.expect(expR.U)
        dut.io.g.expect(expG.U)
        dut.io.b.expect(expB.U)
        dut.io.transparent.expect(expTransp.B)
      }
    }
  }
}
