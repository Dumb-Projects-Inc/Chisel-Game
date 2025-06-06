package gameEngine.entity

import java.io.File
import javax.imageio.ImageIO

/** Loads a PNG image from the specified file path and processes its pixel data.
  *
  * @param filepath
  *   The path to the PNG image file.
  * @return
  *   A tuple containing:
  *   - A sequence of BigInt values, each encoding one pixel's transparency and
  *     color information. 13 bit value where MSB is transparency flag, and the
  *     last 4x3 bits are respectively r,g and b
  *   - The width of the image.
  *   - The height of the image.
  */
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
      val alpha = (rgb >> 24) & 0xff
      val transparentBit = if (alpha == 0) 1 else 0

      val r4 = r / 16
      val g4 = g / 16
      val b4 = b / 16
      BigInt((transparentBit << 12) | (r4 << 8) | (g4 << 4) | b4)
    }).toIndexedSeq
    (pixels, width, height)
  }
}
