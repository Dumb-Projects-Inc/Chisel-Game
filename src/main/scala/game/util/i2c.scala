package gameEngine.util

import chisel3._
import chisel3.experimental.Analog

class I2CInterface extends Bundle {
  val sda = Analog(1.W)
  val scl = Analog(1.W)
}
