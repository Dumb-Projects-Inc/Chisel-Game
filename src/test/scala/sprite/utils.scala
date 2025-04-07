import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import gameEngine.sprite._

class SpriteImageUtilSpec extends AnyFlatSpec with Matchers {

  "SpriteImageUtil.loadPngData" should "correctly load a 3x3 sprite with expected colors" in {
    // test_sprite is is a 3x3 png
    val filepath = "src/test/resources/test_sprite.png"
    val (pixels, width, height) = SpriteImageUtil.loadPngData(filepath)

    width shouldEqual 3
    height shouldEqual 3

    // expected pixel values starting with the top column left to right
    val expectedPixels = Seq(
      BigInt("F00", 16),
      BigInt("3F0", 16),
      BigInt("04F", 16),
      BigInt("FFF", 16),
      BigInt("FFF", 16),
      BigInt("FFF", 16),
      BigInt("000", 16),
      BigInt("000", 16),
      BigInt("000", 16)
    )

    pixels shouldEqual expectedPixels
  }
}
