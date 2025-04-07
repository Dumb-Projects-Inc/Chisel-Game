package gameEngine.sprite

import java.io.File
import javax.imageio.ImageIO

object SpriteImageUtil {
  def loadPngData(filepath: String): (Seq[BigInt], Int, Int) = {
    val img = ImageIO.read(new java.io.File(filepath))
    val width = img.getWidth
    val height = img.getHeight
    val pixels: IndexedSeq[BigInt] = (for {
      y <- 0 until height
      x <- 0 until width
    } yield {
      val rgb = img.getRGB(x, y)
      val r = (rgb >> 16) & 0xff
      val g = (rgb >> 8) & 0xff
      val b = rgb & 0xff

      val r4 = r >> 4
      val g4 = g >> 4
      val b4 = b >> 4
      BigInt((r4 << 8) | (g4 << 4) | b4)
    }).toIndexedSeq
    (pixels, width, height)
  }
}
