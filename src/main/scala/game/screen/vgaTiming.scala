package gameEngine.screen

import chisel3._
import chisel3.util._

class VGATiming extends Module() {
    val io = IO(new Bundle {
        val hSync = Output(Bool())
        val vSync = Output(Bool())
        val visible = Output(Bool())
        val pixelX = Output(UInt(10.W))
        val pixelY = Output(UInt(10.W))
    })

    val hSync = WireInit(false.B)
    val vSync = WireInit(false.B)
    val visible = WireInit(false.B)

    //TODO: Find a way to import settings from VGAController
    // VGA timing parameters for 640x480 @ 60Hz
    val hVisible = 640
    val hFrontPorch = 16
    val hSyncPulse = 96
    val hBackPorch = 48
    val hTotal = hVisible + hFrontPorch + hSyncPulse + hBackPorch

    val vVisible = 480
    val vFrontPorch = 10
    val vSyncPulse = 2
    val vBackPorch = 33
    val vTotal = vVisible + vFrontPorch + vSyncPulse + vBackPorch

  // Horizontal and vertical counters
  val hCounter = RegInit(0.U(log2Ceil(hTotal).W))
  val vCounter = RegInit(0.U(log2Ceil(vTotal).W))

  // Horizontal counter logic
  when(hCounter === (hTotal - 1).U) {
    hCounter := 0.U
    when(vCounter === (vTotal - 1).U) {
      vCounter := 0.U
    }.otherwise {
      vCounter := vCounter + 1.U
    }
  }.otherwise {
    hCounter := hCounter + 1.U
  }

    // Generate sync signals
    hSync := !(hCounter >= (hVisible + hFrontPorch).U && hCounter < (hVisible + hFrontPorch + hSyncPulse).U)
    vSync := !(vCounter >= (vVisible + vFrontPorch).U && vCounter < (vVisible + vFrontPorch + vSyncPulse).U)

    visible := (hCounter < hVisible.U) && (vCounter < vVisible.U)

    io.hSync := hSync
    io.vSync := vSync
    io.visible := visible
    io.pixelX := hCounter
    io.pixelY := vCounter    
}