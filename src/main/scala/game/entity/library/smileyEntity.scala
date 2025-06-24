package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.entity.PalettedSpriteEntity

class BobEntity(
    coordWidth: Int,
    palette: Seq[UInt],
    period: Int
) extends Module {
  val io = IO(new Bundle {
    val screen = new Bundle {
      val x = Input(UInt(coordWidth.W))
      val y = Input(UInt(coordWidth.W))
    }
    val setPos = new Bundle {
      val wrEn = Input(Bool())
      val x = Input(UInt(coordWidth.W))
      val y = Input(UInt(coordWidth.W))
    }
    val scale = Input(UInt(12.W))
    val visible = Output(Bool())
    val pixel = Output(UInt(log2Ceil(palette.length).W))
  })

  val posX = RegInit(0.U(coordWidth.W))
  val posY = RegInit(0.U(coordWidth.W))
  val prevScale = RegInit(io.scale)

  // Animation states
  object State {
    val Idle = 0.U(3.W)
    val Left = 1.U(3.W)
    val Right = 2.U(3.W)
    val Towards = 3.U(3.W)
    val Away = 4.U(3.W)
  }
  val stateReg = RegInit(State.Idle)

  val moved = io.setPos.wrEn
  val dxPos = io.setPos.x =/= posX
  val dyPos = io.setPos.y =/= posY
  val scaleUp = io.scale > prevScale
  val scaleDown = io.scale < prevScale

  prevScale := io.scale
  when(io.setPos.wrEn) {
    posX := io.setPos.x
    posY := io.setPos.y
  }

  // Frame toggle logic that toggles between 1 and 0 every input period.
  private val cntWidth = log2Ceil(period + 1)
  val frameCnt = RegInit(0.U(cntWidth.W))
  val frameToggle = RegInit(false.B)

  frameCnt := Mux(frameCnt === (period - 1).U, 0.U, frameCnt + 1.U)
  when(frameCnt === (period - 1).U) {
    frameToggle := ~frameToggle
  }

  // A function to make it easier to instatiate sprites
  def mkSprite(file: String) = {
    val m = Module(
      new PalettedSpriteEntity(
        filename = file,
        coordWidth = coordWidth,
        palette = palette,
        imageWidth = 16,
        imageHeight = 16
      )
    )
    m.io.in.pos.wrEn.get := true.B

    // Changed to halfScaledX to center the scaling in the sprite instead of it scaling from point 0x0
    val halfScaledW = (16.U * io.scale) / 200.U
    val halfScaledH = (16.U * io.scale) / 200.U
    m.io.in.pos.x := posX - halfScaledW
    m.io.in.pos.y := posY - halfScaledH
    m.scale := io.scale
    m.io.in.speed.wrEn.get := false.B
    m.io.in.acceleration.wrEn.get := false.B
    m.io.in.speed.x := DontCare
    m.io.in.speed.y := DontCare
    m.io.in.acceleration.x := DontCare
    m.io.in.acceleration.y := DontCare
    m
  }

  // Idle
  val sprI1 = mkSprite("sprites/WalkingGuy/Idle1.png")
  val sprI2 = mkSprite("sprites/WalkingGuy/Idle2.png")

  // Walking
  val sprLL = mkSprite("sprites/WalkingGuy/Walking-left1.png")
  val sprL2 = mkSprite("sprites/WalkingGuy/Walking-left2.png")
  val sprRL = mkSprite("sprites/WalkingGuy/Walking-right1.png")
  val sprR2 = mkSprite("sprites/WalkingGuy/Walking-right2.png")
  val sprTL = mkSprite("sprites/WalkingGuy/Walking-towards1.png")
  val sprT2 = mkSprite("sprites/WalkingGuy/Walking-towards2.png")
  val sprAL = mkSprite("sprites/WalkingGuy/Walking-away1.png")
  val sprA2 = mkSprite("sprites/WalkingGuy/Walking-away2.png")

  // Connect screen coords to all sprites
  val allSprites =
    Seq(sprI1, sprI2, sprLL, sprL2, sprRL, sprR2, sprTL, sprT2, sprAL, sprA2)
  for (sprite <- allSprites) {
    sprite.io.in.screen.x := io.screen.x
    sprite.io.in.screen.y := io.screen.y
  }

  val (idxs, masks) =
    allSprites.map(m => (m.io.pixel, m.io.visible === false.B)).unzip

  val pixels = VecInit(idxs)
  val transparents = VecInit(masks)

  // This part ONLY works if there is 2 frames in each animation state.
  // If there is more or less a modification or totally different logic needs to be applied.
  val sel = stateReg * 2.U + frameToggle.asUInt
  io.pixel := pixels(sel)
  io.visible := !transparents(sel)
}
