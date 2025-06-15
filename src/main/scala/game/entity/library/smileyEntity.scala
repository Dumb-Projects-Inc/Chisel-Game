package gameEngine.entity.library

import chisel3._
import chisel3.util._
import gameEngine.entity.SpriteEntity
import gameEngine.entity.PalettedSpriteEntity

class ScreenIO(width: Int) extends Bundle {
  val x = Input(UInt(width.W))
  val y = Input(UInt(width.W))
}

class SmileyEntity(width: Int, palette: Seq[UInt]) extends Module {
  val io = IO(new Bundle {
    val screen = new Bundle {
      val x = Input(UInt(width.W))
      val y = Input(UInt(width.W))
    }
    val setPos = new Bundle {
      val wrEn = Input(Bool())
      val x = Input(UInt(width.W))
      val y = Input(UInt(width.W))
    }
    val scale = Input(SInt())
    val visible = Output(Bool())
    val pixel = Output(UInt(12.W))
  })

  val posX = RegInit(0.U(width.W))
  val posY = RegInit(0.U(width.W))

  val sprite = Module(new PalettedSpriteEntity("./smiley-64.png", width, palette, 64, 64))
  sprite.scale := io.scale

  when(io.setPos.wrEn) {
    posX := io.setPos.x
    posY := io.setPos.y
  }

  sprite.io.in.screen.x := io.screen.x
  sprite.io.in.screen.y := io.screen.y

  sprite.io.in.pos.wrEn.get := true.B
  sprite.io.in.pos.x := posX
  sprite.io.in.pos.y := posY

  sprite.io.in.speed.wrEn.get := false.B
  sprite.io.in.acceleration.wrEn.get := false.B

  sprite.io.in.speed.x := DontCare
  sprite.io.in.speed.y := DontCare
  sprite.io.in.acceleration.x := DontCare
  sprite.io.in.acceleration.y := DontCare

  io.visible := sprite.io.visible
  io.pixel := sprite.io.pixel
}