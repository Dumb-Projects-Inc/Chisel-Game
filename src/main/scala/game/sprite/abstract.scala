package gameEngine.sprite

import chisel3._
import chisel3.util.log2Ceil

/** Abstract Sprite module.
  * @param width
  *   Width of the sprite (in pixels).
  * @param height
  *   Height of the sprite (in pixels).
  *
  * Request a color at x,y value by driving inputs. The r,g,b value comes out as
  * outputs. A transparent output is also given, indicating that the pixel at
  * x,y is transparent. The rbg value of (0,0,0) / 0x000 is reserved for this
  */
abstract class Sprite(val width: Int, val height: Int) extends Module {
  val io = IO(new Bundle {
    val x = Input(UInt(log2Ceil(width).W))
    val y = Input(UInt(log2Ceil(width).W))
    val r, g, b = Output(UInt(4.W))
    val transparent = Output(Bool())
  })
}

class ImageSprite(filepath: String, width: Int, height: Int)
    extends Sprite(width, height) {
  val (pixelData, imgWidth, imgHeight) = SpriteImageUtil.loadPngData(filepath)

  require(
    imgWidth == width && imgHeight == height,
    s"Image dimensions mismatch: expected $width x $height, got $imgWidth x $imgHeight"
  )

  val rom = VecInit.tabulate(height, width) { (row, col) =>
    pixelData(row * width + col).U(12.W)
  }

  val rgb = rom(io.x)(io.y)
  io.r := rgb(11, 8)
  io.g := rgb(7, 4)
  io.b := rgb(3, 0)
  io.transparent := rgb === 0.U
}
