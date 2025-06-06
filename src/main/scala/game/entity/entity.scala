package gameEngine.entity

import chisel3._
import chisel3.util._
import os.write

class IEntity(width: Int, writeable: Boolean = false) extends Bundle {
  val screen = new Bundle {
    val x = UInt(width.W)
    val y = UInt(width.W)
  }
  val pos = new Bundle {
    val wrEn = if (writeable) Some(new Bool()) else None
    val x = UInt(width.W)
    val y = UInt(width.W)
  }
  val speed = new Bundle {
    val wrEn = if (writeable) Some(new Bool()) else None
    val x = SInt((width + 1).W)
    val y = SInt((width + 1).W)
  }
  val acceleration = new Bundle {
    val wrEn = if (writeable) Some(new Bool()) else None
    val x = SInt((width + 1).W)
    val y = SInt((width + 1).W)
  }
}

class Entity(width: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(new IEntity(width, writeable = true))
    val out = Output(new IEntity(width))
    val visible = Output(Bool())
    val pixel = Output(UInt(12.W))
  })

  val posRegx = RegInit(0.U(width.W))
  val posRegy = RegInit(0.U(width.W))

  val speedRegx = RegInit(0.S(width.W))
  val speedRegy = RegInit(0.S(width.W))
  val accelerationRegx = RegInit(0.S(width.W))
  val accelerationRegy = RegInit(0.S(width.W))

  posRegx := Mux(
    io.in.pos.wrEn.getOrElse(false.B),
    io.in.pos.x,
    (posRegx.asSInt + speedRegx).asUInt
  )
  posRegy := Mux(
    io.in.pos.wrEn.getOrElse(false.B),
    io.in.pos.y,
    (posRegy.asSInt + speedRegy).asUInt
  )

  speedRegx := Mux(
    io.in.speed.wrEn.getOrElse(false.B),
    io.in.speed.x,
    speedRegx + accelerationRegx
  )
  speedRegy := Mux(
    io.in.speed.wrEn.getOrElse(false.B),
    io.in.speed.y,
    speedRegy + accelerationRegy
  )

  accelerationRegx := Mux(
    io.in.acceleration.wrEn.getOrElse(false.B),
    io.in.acceleration.x,
    accelerationRegx
  )

  accelerationRegy := Mux(
    io.in.acceleration.wrEn.getOrElse(false.B),
    io.in.acceleration.y,
    accelerationRegy
  )

  io.out.screen := io.in.screen
  io.out.pos.x := posRegx
  io.out.pos.y := posRegy
  io.out.speed.x := speedRegx
  io.out.speed.y := speedRegy
  io.out.acceleration.x := accelerationRegx
  io.out.acceleration.y := accelerationRegy
}


class SpriteEntity(filename: String, width: Int) extends Entity(width) {
  val sprite = Module(new ImageSprite(filename, 64, 64))

  sprite.io.x := io.in.screen.x - posRegx
  sprite.io.y := io.in.screen.y - posRegy

  io.visible := io.in.screen.x - posRegx < 64.U && io.in.screen.y - posRegy < 64.U && !sprite.io.transparent
  io.pixel := sprite.io.r ## sprite.io.g ## sprite.io.b
}
