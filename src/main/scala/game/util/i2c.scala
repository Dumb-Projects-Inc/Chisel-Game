package gameEngine.util 

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog


class I2CInterface extends Bundle {
  val sda = Analog(1.W)
  val scl = Analog(1.W)
}

class I2C extends Module {
  val io = IO(new I2CInterface)

  //I2C pins
    val sda = Wire(Analog(1.W))

    val scl = Wire(Analog(1.W))

    io.sda <> sda
    io.scl <> scl

    //I2C signals
    val i2c_sda = RegInit(true.B)
    val i2c_scl = RegInit(true.B)

    //I2C state machine
    val idle :: start :: write :: read :: ack :: stop :: Nil = Enum(6)
    val state = RegInit(idle)

    switch(state) {
      
    }

}