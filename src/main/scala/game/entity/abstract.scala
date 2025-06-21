package gameEngine.entity

import chisel3._
import chisel3.util.log2Ceil
import scala.math.min

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

import chisel3._

class PalettedIndexSprite(
    filepath: String,
    imageWidth: Int,
    imageHeight: Int,
    palette: Seq[UInt],
    maxRenderWidth: Int,
    maxRenderHeight: Int
) extends Module {
  val io = IO(new Bundle {
    val x = Input(UInt(log2Ceil(maxRenderWidth).W))
    val y = Input(UInt(log2Ceil(maxRenderHeight).W))
    val scale = Input(UInt(12.W))
    val idx = Output(UInt(log2Ceil(palette.length).W))
    val transparent = Output(Bool())
  })

  private def dist(c1: Int, c2: Int): Int = {
    val (r1, g1, b1) = ((c1 >> 8) & 0xf, (c1 >> 4) & 0xf, c1 & 0xf)
    val (r2, g2, b2) = ((c2 >> 8) & 0xf, (c2 >> 4) & 0xf, c2 & 0xf)
    val dr = r1 - r2; val dg = g1 - g2; val db = b1 - b2
    dr * dr + dg * dg + db * db
  }
  val (pixelData, imgW, imgH) = SpriteImageUtil.loadPngData(filepath)
  require(imgW == imageWidth && imgH == imageHeight)
  private val palCodes = palette.map(_.litValue.toInt).toArray
  private val palWithIdx = palCodes.zipWithIndex

  val baseIdx = Array.ofDim[Int](imageHeight, imageWidth)
  val baseT = Array.ofDim[Boolean](imageHeight, imageWidth)
  for (y <- 0 until imageHeight; x <- 0 until imageWidth) {
    val raw = pixelData(y * imageWidth + x).toInt
    val t = ((raw >> 12) & 1) != 0
    baseT(y)(x) = t
    baseIdx(y)(x) =
      if (t) 0
      else palWithIdx.minBy { case (c, _) => (dist(raw & 0xfff, c)) }._2
  }
  val idxROM = VecInit(
    baseIdx.toIndexedSeq.map { row =>
      VecInit(row.toIndexedSeq.map(_.U(log2Ceil(palette.length).W)))
    }
  )
  val maskROM = VecInit(
    baseT.toIndexedSeq.map { row =>
      VecInit(row.toIndexedSeq.map(_.B))
    }
  )

  val validScales = 5 to 1000
  private val recipWidth = 19
  val recipLUT = VecInit(validScales.map { s =>
    ((100 << 14) / s).U(recipWidth.W)
  })

  val s0 = Mux(io.scale < 5.U, 5.U, Mux(io.scale > 1000.U, 1000.U, io.scale))
  val idxScale = s0 - 5.U
  val recip0 = recipLUT(idxScale)

  val x0 = io.x;
  val y0 = io.y

  val rx1 = (RegNext(x0) * RegNext(recip0)) >> 14
  val ry1 = (RegNext(y0) * RegNext(recip0)) >> 14
  val cx2 = RegNext(Mux(rx1 >= imageWidth.U, (imageWidth - 1).U, rx1))
  val cy2 = RegNext(Mux(ry1 >= imageHeight.U, (imageHeight - 1).U, ry1))

  val scaledW = (imageWidth.U * s0) / 100.U
  val scaledH = (imageHeight.U * s0) / 100.U

  // Quick fix for strange outside sprite bars (makes them transparent)
  val outside = (x0 >= scaledW) || (y0 >= scaledH)

  val rawIdx = idxROM(cy2)(cx2)
  val rawMask = maskROM(cy2)(cx2)
  val finalMask = outside || rawMask
  val finalIdx = Mux(finalMask, 0.U, rawIdx)

  io.idx := RegNext(finalIdx)
  io.transparent := RegNext(finalMask)
}
