package gameEngine.screen

import chisel3._

class VGATiming extends Module() {
    val io = IO(new Bundle {
        val hSync = Output(Bool())
        val vSync = Output(Bool())
        val visible = Output(Bool())
        val pixelX = Output(UInt(10.W))
        val pixelY = Output(UInt(10.W))
    })


    val hCounter = RegInit(0.U(10.W))
    val vCounter = RegInit(0.U(10.W))


    val hSync = WireInit(false.B)
    val vSync = WireInit(false.B)
    val visible = WireInit(false.B)

    //TODO: Find a way to import settings from VGAController
    // VGA timing parameters for 640x480 @ 60Hz
    val hVisible = 640.U
    val hFrontPorch = 16.U
    val hSyncPulse = 96.U
    val hBackPorch = 48.U
    val hTotal = hVisible + hFrontPorch + hSyncPulse + hBackPorch

    val vVisible = 480.U
    val vFrontPorch = 11.U
    val vSyncPulse = 2.U
    val vBackPorch = 31.U
    val vTotal = vVisible + vFrontPorch + vSyncPulse + vBackPorch


    //Increment the counters
    when(hCounter === hTotal - 1.U) {
        hCounter := 0.U
        when(vCounter === vTotal - 1.U) {
            vCounter := 0.U
        }.otherwise {
            vCounter := vCounter + 1.U
        }
    }.otherwise {
        hCounter := hCounter + 1.U
    }


    visible := (hCounter < hVisible) && (vCounter < vVisible)
    // Generate hsync signal (active low)
    hSync := !(hCounter >= (hVisible + hFrontPorch) && hCounter < (hVisible + hFrontPorch + hSyncPulse))

    // Generate vsync signal (active low)
    vSync := !(vCounter >= (vVisible + vFrontPorch) && vCounter < (vVisible + vFrontPorch + vSyncPulse))

    io.hSync := hSync
    io.vSync := vSync
    io.visible := visible
    io.pixelX := hCounter
    io.pixelY := vCounter    
}