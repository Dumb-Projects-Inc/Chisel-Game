package gameEngine.entity

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
    val y = Input(UInt(log2Ceil(height).W))
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
    pixelData(row * width + col).U(13.W)
  }

  val rgb = rom(io.y)(io.x)
  io.r := rgb(11, 8)
  io.g := rgb(7, 4)
  io.b := rgb(3, 0)
  io.transparent := rgb(12)
}

class PalettedIndexSprite(
    filepath: String,
    width: Int,
    height: Int,
    palette: Seq[UInt]
) extends Module {
  val io = IO(new Bundle {
    val x, y = Input(UInt(log2Ceil(width).W))
    val idx = Output(UInt(log2Ceil(palette.length).W))
    val transparent = Output(Bool())
    val scale = Input(SInt())
  })

  val (pixelData, imgW, imgH) = SpriteImageUtil.loadPngData(filepath)
  require(imgW == width && imgH == height)

  private def dist(c1: Int, c2: Int) = {
    val r1 = (c1 >> 8) & 0xf
    val g1 = (c1 >> 4) & 0xf
    val b1 = c1 & 0xf
    val r2 = (c2 >> 8) & 0xf
    val g2 = (c2 >> 4) & 0xf
    val b2 = c2 & 0xf
    val dr = r1 - r2; val dg = g1 - g2; val db = b1 - b2
    dr * dr + dg * dg + db * db
  }

  private val palCodes = palette.map(_.litValue.toInt).toArray
  private val palWithIdx = palCodes.zipWithIndex

  private val quantIdx = Array.fill(width * height)(0)
  private val quantT = Array.fill(width * height)(false)
  for (((rawBig, i)) <- pixelData.zipWithIndex) {
    val raw = rawBig.toInt
    val t = ((raw >> 12) & 1) != 0
    quantT(i) = t
    quantIdx(i) =
      if (t) 0
      else {
        val rgb12 = raw & 0xfff
        palWithIdx.minBy { case (code, _) => dist(rgb12, code) }._2
      }
  }

  val idxRom = VecInit.tabulate(height, width) { (r, c) =>
    quantIdx(r * width + c).U
  }
  val maskRom = VecInit.tabulate(height, width) { (r, c) =>
    quantT(r * width + c).B
  }

  val sx_s = io.x.asSInt * 100.S / io.scale
  val sy_s = io.y.asSInt * 100.S / io.scale

  val sx = Mux(sx_s < 0.S, 0.U, (sx_s.asUInt min (width - 1).U))
  val sy = Mux(sy_s < 0.S, 0.U, (sy_s.asUInt min (height - 1).U))
  val inBounds = sx < width.U && sy < height.U

  val pixIdx = Mux(inBounds, idxRom(sy)(sx), 0.U)
  val pixTr = Mux(inBounds, maskRom(sy)(sx), true.B)

  io.idx := pixIdx
  io.transparent := pixTr
}
