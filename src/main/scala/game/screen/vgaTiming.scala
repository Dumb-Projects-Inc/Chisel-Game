package gameEngine.screen

import chisel3._
import chisel3.util._

case class VgaConfig(
    visibleAreaH: Int,
    visibleAreaV: Int,
    frontPorchH: Int,
    frontPorchV: Int,
    syncPulseH: Int,
    syncPulseV: Int,
    backPorchH: Int,
    backPorchV: Int
)

object VgaConfigs {
  val vga640x480 = VgaConfig(
    visibleAreaH = 640,
    visibleAreaV = 480,
    frontPorchH = 16,
    frontPorchV = 10,
    syncPulseH = 96,
    syncPulseV = 2,
    backPorchH = 48,
    backPorchV = 33
  )
  val vga1280x1024 = VgaConfig(
    visibleAreaH = 1280,
    visibleAreaV = 1024,
    frontPorchH = 48,
    frontPorchV = 1,
    syncPulseH = 112,
    syncPulseV = 3,
    backPorchH = 248,
    backPorchV = 38
  )
}

class VGATiming(
    config: VgaConfig = VgaConfigs.vga640x480
) extends Module() {
  import config._
  val io = IO(new Bundle {
    val hSync, vSync, visible = Output(Bool())
    val pixelX, pixelY =
      Output(UInt(log2Ceil(math.max(visibleAreaH, visibleAreaV)).W))
  })

  val totalH = frontPorchH + visibleAreaH + syncPulseH + backPorchH - 1
  val startHSync = frontPorchH + visibleAreaH - 1
  val endHSync = startHSync + syncPulseH - 1

  val totalV = frontPorchV + visibleAreaV + syncPulseV + backPorchV - 1
  val startVSync = frontPorchV + visibleAreaV - 1
  val endVSync = startVSync + syncPulseV - 1

  val hCounter = RegInit(0.U(log2Ceil(totalH).W))
  val vCounter = RegInit(0.U(log2Ceil(totalV).W))

  when(hCounter === (totalH).U) {
    hCounter := 0.U
    when(vCounter === (totalV).U) {
      vCounter := 0.U
    }.otherwise {
      vCounter := vCounter + 1.U
    }
  }.otherwise {
    hCounter := hCounter + 1.U
  }

  val hSync = Wire(Bool())
  val vSync = Wire(Bool())
  val visible = Wire(Bool())
  hSync := (hCounter >= startHSync.U) && (hCounter <= endHSync.U)
  vSync := (vCounter >= startVSync.U) && (vCounter <= endVSync.U)
  visible := (hCounter < visibleAreaH.U) && (vCounter < visibleAreaV.U)

  io.hSync := hSync
  io.vSync := vSync
  io.visible := visible
  io.pixelX := hCounter
  io.pixelY := vCounter

}
