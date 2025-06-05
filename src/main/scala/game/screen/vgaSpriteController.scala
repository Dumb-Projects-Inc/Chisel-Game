package gameEngine.screen

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import gameEngine.util._
import gameEngine.screen._
import gameEngine.sprite._

case class SpriteSpec(
  filepath: String,
  width:    Int,
  height:   Int
)

class SpritePosIO(n: Int) extends Bundle {
  val x = Input(Vec(n, UInt(10.W)))
  val y = Input(Vec(n, UInt(10.W)))
}

class VGASpriteController(specs: Seq[SpriteSpec], i2cEn: Boolean = false) extends Module {
  val n = specs.length

  val io = IO(new Bundle {
    val vga = new VGAInterface(i2cEn)
    val pos = new SpritePosIO(n)
  })

  val pll    = Module(new PLLBlackBox)
  pll.io.clock := clock
  pll.io.reset := reset
  val clk25   = pll.io.clk25MHz
  val locked  = pll.io.locked

  withClockAndReset(clk25, !locked) {
    val timing = Module(new VGATiming)
    io.vga.hsync := timing.io.hSync
    io.vga.vsync := timing.io.vSync
    val visible = timing.io.visible
    val x       = timing.io.pixelX
    val y       = timing.io.pixelY

    val roms = specs.map { s =>
      Module(new ImageSprite(s.filepath, s.width, s.height)) // Creating one ROM per sprite (one per spec)
    }

    // Fold means to iterate over the specs and their index and roms
    // Here we are using a foldLeft to get the pixel value for each sprite top to bottom
    // and combine them into a single pixel value
    val pixel = specs.zipWithIndex.zip(roms).foldLeft(0.U(12.W)) { 
      case (acc, ((spec, idx), rom)) =>
        // dx and dy are the x and y positions of the sprite
        // AKA the top left corner of the sprite
        val dx = io.pos.x(idx)
        val dy = io.pos.y(idx)

        // Checking of the pixels are inside the sprite area
        val inX = x >= dx && x < (dx + spec.width.U)
        val inY = y >= dy && y < (dy + spec.height.U)
        
        // Which we use here to tell the rom to read the pixel data
        // Or set it to 0 if not in the sprite area
        rom.io.x := Mux(inX, x - dx, 0.U)
        rom.io.y := Mux(inY, y - dy, 0.U)

        val rgb = Cat(rom.io.r, rom.io.g, rom.io.b)

        // Lastly we see if we are inside the sprite and if the sprite is not transparent
        // If none of that is true we set the pixel to what was there before
        // If it is true we set the pixel to the new value 
        Mux(inX && inY && !rom.io.transparent, rgb, acc)
    }

    // pixelOut handles if the pixel is visible or not and sets the pixel to 0 if not visible 
    val pixelOut = Mux(visible, pixel, 0.U(12.W))
    io.vga.red   := pixelOut(11, 8)
    io.vga.green := pixelOut( 7, 4)
    io.vga.blue  := pixelOut( 3, 0)  
  }
}
