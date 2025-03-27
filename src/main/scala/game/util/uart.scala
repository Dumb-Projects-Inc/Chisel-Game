package gameEngine.util

import chisel3._

class UARTInterface extends Bundle {
  val tx = Output(Bool())
  val rx = Input(Bool())
}

class UART extends Module {
    val io = IO(new UARTInterface)

}