---
id: vga-sprite-controller
title: VGA Sprite Controller
---

# VGA Sprite Controller

The `VGASpriteController` is a Chisel module that takes in a list of `SpriteSpec` definitions and renders them to a VGA display, drawing each sprite at dynamic (x, y) positions. It supports multiple sprites, layering them from first to last.

---

## Inputs

### Sprite Specifications

Each sprite is specified with:

```scala
case class SpriteSpec(
  filepath: String,
  width:    Int,
  height:   Int
)
```

These are passed into the controller at instantiation:

```scala mdoc
val specs = Seq(
  SpriteSpec("sprite1.png", 16, 16),
  SpriteSpec("sprite2.png", 32, 32)
)
```

---

## Core Logic

### VGA Pixel Timing

The controller utilizes the `PLLBlackBox` modue to operate with a 25Mhz clock instead of the Basys3 100MHz clock.
With the use of `withClockAndReset` it switches to this clock and the `!locked` effectively describes holding in reset until the PLL locks.

```scala mdoc
val pll = Module(new PLLBlackBox)
pll.io.clock := clock
pll.io.reset := reset
val clk25 = pll.io.clk25MHz
val locked = pll.io.locked

withClockAndReset(clk25, !locked) {
  val timing = Module(new VGATiming)
  val x = timing.io.pixelX
  val y = timing.io.pixelY
  val visible = timing.io.visible

  io.vga.hsync := timing.io.hSync
  io.vga.vsync := timing.io.vSync
```

### Sprite Rendering Logic

For each sprite that is defined inside specs it iterates through that and creates a ROM. This happens from top to bottom of the specs list of sprites that are defined. This means that the list indexes describes the layer priority. 

After that the `pixel` val will combine the combine the sprites pixel values for each pixel and then for each clock cycle check if its inside a sprite area, and then write that sprite pixel if it's not set as transparent.

```scala mdoc
 val roms = specs.map { s =>
      Module(new ImageSprite(s.filepath, s.width, s.height))
    }

    val pixel = specs.zipWithIndex.zip(roms).foldLeft(0.U(12.W)) {
      case (acc, ((spec, idx), rom)) =>
        val dx = io.pos.x(idx)
        val dy = io.pos.y(idx)

        val inX = x >= dx && x < (dx + spec.width.U)
        val inY = y >= dy && y < (dy + spec.height.U)

        rom.io.x := Mux(inX, x - dx, 0.U)
        rom.io.y := Mux(inY, y - dy, 0.U)

        val rgb = Cat(rom.io.r, rom.io.g, rom.io.b)

        Mux(inX && inY && !rom.io.transparent, rgb, acc)
    }

    val pixelOut = Mux(visible, pixel, 0.U(12.W))
    io.vga.red   := pixelOut(11, 8)
    io.vga.green := pixelOut( 7, 4)
    io.vga.blue  := pixelOut( 3, 0)
  }
```
## Example of usage

The below usage is a simple example to show how to load 3 sprites in 3 different positions with 3 different formats.

The val `counter` shows a simple way to update a specific sprite position while keeping the other static. In this example you will see that the ***finger-48*** sprite moves over the ***smiley-64*** in a loop which shows the layering priority earlier described.

```scala mdoc
class Engine extends Module {
  val io = IO(new Bundle {
    val vga = new VGAInterface
  })

  val specs = Seq(
    SpriteSpec("src/main/resources/smiley-64.png", 64, 64),
    SpriteSpec("src/main/resources/finger-48.png", 48, 48),
    SpriteSpec("src/main/resources/DoomUI-640-70.png", 640, 70)
  )
  val posX = RegInit(VecInit(Seq(0.U(10.W), 100.U(10.W), 0.U(10.W))))
  val posY = RegInit(VecInit(Seq(0.U(10.W),  50.U(10.W), 410.U(10.W))))

  val counter = RegInit(0.U(26.W))
  counter := counter + 1.U
  when(counter === (1_000_000 - 1).U) {
    counter := 0.U
    posX(1) := Mux(posX(1) === 0.U, 100.U, posX(1) - 1.U)
  }
  val vgaCtrl = Module(new VGASpriteController(specs))

  io.vga <> vgaCtrl.io.vga

  vgaCtrl.io.pos.x := posX
  vgaCtrl.io.pos.y := posY
}
```
---

## IO Description

### Parameters

| Name    | Type                   | Description                              |
| ------- | ---------------------- | ---------------------------------------- |
| `specs` | `Seq[SpriteSpec]`      | List of `(filepath, width, height)` tuples for each sprite image |
| `i2cEn` | `Boolean`              | **NOT IMPLEMENTED** |

### Inputs

| Port           | Direction | Type               | Width         | Description          |
| -------------- | --------- | ------------------ | ------------- | -------------------- |
| `io.pos.x`     | Input     | `Vec(n, UInt)`     | n × 10 bits   | Sprite X coordinates |
| `io.pos.y`     | Input     | `Vec(n, UInt)`     | n × 10 bits   | Sprite Y coordinates |

### Outputs

| Port             | Direction | Type               | Width         | Description           |
| ---------------- | --------- | ------------------ | ------------- | --------------------- |
| `io.vga.red`     | Output    | `UInt`             | 4 bits (11:8) | Red channel (MSBs)    |
| `io.vga.green`   | Output    | `UInt`             | 4 bits (7:4)  | Green channel         |
| `io.vga.blue`    | Output    | `UInt`             | 4 bits (3:0)  | Blue channel (LSBs)   |
| `io.vga.hsync`   | Output    | `Bool` (or `UInt`) | 1 bit         | Horizontal sync pulse |
| `io.vga.vsync`   | Output    | `Bool` (or `UInt`) | 1 bit         | Vertical sync pulse   |

---

