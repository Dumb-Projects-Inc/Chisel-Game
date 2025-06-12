package gameEngine.screen

import chisel3._
import chisel3.util._

/** Case class that stores the configuration parameters for VGA timing. Source:
  * https://martin.hinner.info/vga/timing.html
  */
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

}

/** Chisel module that generates VGA timing signals for 640x480x60hz
  *
  * IMPORTANT: Expects a 100mhz clock
  *
  * The purpose of this module is to generate the necessary hSync and vSync
  * signal, to be plugged directly to the vga port. It also outputs a x,y
  * coordinate pair, which indicate which position the vga port is currently
  * reading on its r,g,b inputs. A "visible" output is provided for convinience,
  * indicating if the current x,y coordinate pair is on the screen. The pixelX,
  * pixelY and visible outputs should be used by an outside circuit to drive the
  * r,g,b pins of the vga screen.
  */
class VGATiming(config: VgaConfig = VgaConfigs.vga640x480) extends Module {
  import config._
  val io = IO(new Bundle {
    val hSync, vSync, visible = Output(Bool())
    val pixelX, pixelY =
      Output(UInt(log2Ceil(math.max(visibleAreaH, visibleAreaV)).W))
  })

  val (_, tick) = Counter(true.B, 2)

  val totalH = visibleAreaH + frontPorchH + syncPulseH + backPorchH
  val startHSync = visibleAreaH + frontPorchH
  val endHSync = startHSync + syncPulseH

  val totalV = visibleAreaV + frontPorchV + syncPulseV + backPorchV
  val startVSync = visibleAreaV + frontPorchV
  val endVSync = startVSync + syncPulseV

  val (hCounter, hCounterWrap) = Counter(tick, totalH)
  val (vCounter, vCounterWrap) = Counter(hCounterWrap, totalV)

  val hSync = (hCounter >= startHSync.U) && (hCounter < endHSync.U)
  val vSync = (vCounter >= startVSync.U) && (vCounter < endVSync.U)
  val visible = (hCounter < visibleAreaH.U) && (vCounter < visibleAreaV.U)

  io.hSync := hSync
  io.vSync := vSync
  io.visible := visible
  io.pixelX := Mux(visible, hCounter, 0.U)
  io.pixelY := Mux(visible, vCounter, 0.U)

}
