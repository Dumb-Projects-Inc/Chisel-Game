package gameEngine.entity

import chisel3._
import chisel3.util.{
  log2Ceil,
  Counter,
  switch,
  is,
  Enum,
  PriorityMux,
  Queue,
  Decoupled
}

import gameEngine.fixed.FixedPointUtils
import gameEngine.screen.VGAInterface
import gameEngine.framebuffer.DualPaletteFrameBuffer
import gameEngine.entity.library.{ShadedWallEntity, WallSegment}
import gameEngine.raycast._
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.raycast.RayHit
import gameEngine.entity.library.BarrelEntity
import gameEngine.entity.library.BobEntity
import gameEngine.util
import gameEngine.util.uart._

class Scene(playerX: Double = 2.5, playerY: Double = 4.5) extends Module {

  val io = IO(new Bundle {
    val playerAction = Input(PlayerAction())
    val vga = new VGAInterface
    val txd = Output(Bool())
    val rxd = Input(Bool())
  })

  val doomPalette = Seq(
    "h000".U(12.W), // #000000
    "h222".U(12.W), // #222222
    "h444".U(12.W), // #444444
    "h777".U(12.W), // #777777
    "hAAA".U(12.W), // #AAAAAA
    "hFFF".U(12.W), // #FFFFFF

    "h430".U(12.W), // #443300
    "h640".U(12.W), // #664400
    "hA50".U(12.W), // #AA5500
    "hD90".U(12.W), // #DD9900

    "hF20".U(12.W), // #FF2200
    "hF60".U(12.W), // #FF6600
    "hFF0".U(12.W), // #FFFF00

    "hA00".U(12.W), // #AA0000
    "h0F0".U(12.W), // #FF0000
    "h08F".U(12.W) // #0088FF
  )

  val _map: Seq[Seq[Int]] = Seq(
    Seq(1, 1, 1, 1, 1, 1, 1, 1, 1),
    Seq(1, 0, 1, 0, 1, 0, 0, 1, 1),
    Seq(1, 0, 1, 0, 0, 0, 1, 0, 1),
    Seq(1, 0, 1, 0, 1, 0, 1, 0, 1),
    Seq(1, 0, 0, 0, 1, 0, 0, 0, 1),
    Seq(1, 1, 1, 1, 1, 1, 1, 1, 1)
  )
  val nTiles = 2

  val map = VecInit.tabulate(_map.length, _map(0).length) { (x, y) =>
    _map(x)(y).B
  }
  val player = Module(
    new PlayerEntity(playerX, playerY, 3 * math.Pi / 4.0, _map)
  )
  val sprite = Module(new SpritePlacer(map = _map))
  val invSpritelen = RegInit(toFP(0.0))

  val rx = Module(new Rx(100_000_000, 115200))
  val txBuf = Module(new BufferedTx(100_000_000, 115200))
  val posExchange = Module(new gameEngine.util.posExchange)

  rx.io.rxd := io.rxd
  txBuf.io.channel <> posExchange.io.out
  posExchange.io.in <> rx.io.channel
  io.txd := txBuf.io.txd

  posExchange.io.playerPos.bits <> player.io.pos
  posExchange.io.playerPos.valid := false.B
  posExchange.io.bobPos.ready := sprite.io.input.ready

  val rc = Module(new RaycasterCore(map = _map))
  val buf = Module(new DualPaletteFrameBuffer(doomPalette))
  val wall = Module(new ShadedWallEntity(doomPalette.length, 8, 9, 320))
  val bob = Module(new BobEntity(9, doomPalette, 25_000_000))

  bob.io.setPos := DontCare
  bob.io.screen := DontCare
  bob.io.scale := 100.U
  bob.io.setPos.wrEn := false.B

  sprite.io.input.valid := false.B
  sprite.io.input.bits := DontCare

  sprite.io.output.ready := false.B

  val segmentsReg = RegInit(VecInit(Seq.fill(320)(WallSegment(240))))
  wall.io.segments := segmentsReg

  buf.io.x := DontCare
  buf.io.y := DontCare
  buf.io.dataIn := DontCare
  buf.io.wEnable := false.B
  buf.io.valid := false.B

  wall.io.x := DontCare
  wall.io.y := DontCare

  rc.io.in.bits := DontCare
  rc.io.columns.ready := false.B
  rc.io.in.valid := false.B

  // Entity management
  object S extends ChiselEnum {
    val idle, updatePlayer, checkWin, render, renderSprite1, renderSprite2,
        draw, win, done = Value
  }

  object RayState extends ChiselEnum {
    val idle, calculate, filling, waiting = Value
  }
  val state = RegInit(S.idle)
  val rayState = RegInit(RayState.idle)

  val (x, xWrap) = Counter(rayState === RayState.filling, 320)
  val (y, yWrap) = Counter(xWrap && (rayState === RayState.filling), 240)

  val (circx, cxwrap) = Counter(sprite.io.output.valid, 320)
  val (circy, cywrap) = Counter(cxwrap, 240)
  val idx = RegInit(0.U(16.W))

  val (winx, wxwrap) = Counter(state === S.win, 320)
  val (winy, wywrap) = Counter(wxwrap, 320)

  val actionPending = RegInit(PlayerAction.idle)
  val hasPendingAction = RegInit(false.B)

  // Player action handling
  when(io.playerAction =/= PlayerAction.idle && !hasPendingAction) {
    actionPending := io.playerAction
    hasPendingAction := true.B
  }
  val defaultArg = Wire(SInt(FixedPointUtils.width.W))
  defaultArg := 0.S

  player.io.action.bits := Mux(
    hasPendingAction,
    actionPending,
    PlayerAction.idle
  )
  player.io.action.valid := false.B
  player.io.actionArg := defaultArg

  val moveSpeed = 0.02 // Default move speed
  when(hasPendingAction) {
    switch(actionPending) {
      is(PlayerAction.moveForward) { defaultArg := toFP(moveSpeed) }
      is(PlayerAction.moveBackward) { defaultArg := toFP(moveSpeed) }
      is(PlayerAction.strafeLeft) { defaultArg := toFP(moveSpeed) }
      is(PlayerAction.strafeRight) { defaultArg := toFP(moveSpeed) }
      is(PlayerAction.turnLeft) { defaultArg := toFP(0.02) }
      is(PlayerAction.turnRight) { defaultArg := toFP(-0.02) }
    }
  }

  switch(state) {
    is(S.idle) {
      // Wait for input or trigger to start updating
      // Transition to updatePlayer or updateEnemies based on input
      buf.io.valid := true.B
      state := S.updatePlayer
    }
    is(S.updatePlayer) {
      hasPendingAction := false.B
      player.io.action.valid := true.B
      when(player.io.action.ready) {
        posExchange.io.playerPos.valid := true.B
        state := S.checkWin
      }
    }

    is(S.checkWin) {
      when(invSpritelen > toFP(0.5)) {
        state := S.win
      }.otherwise {
        state := S.render
      }
    }

    is(S.render) {

      switch(rayState) {
        is(RayState.idle) {
          rc.io.in.valid := true.B
          rc.io.in.bits.start := player.io.pos
          rc.io.in.bits.angle := player.io.angle
          when(rc.io.in.ready) {
            idx := 0.U
            rayState := RayState.calculate
          }
        }
        is(RayState.calculate) {
          rc.io.columns.ready := true.B
          when(rc.io.columns.valid) {
            for (i <- 0 until 320) {
              segmentsReg(i).height := rc.io.columns.bits(i).height
              segmentsReg(i).isHorizontal := rc.io.columns.bits(i).isHorizontal
            }
            rayState := RayState.filling
          }

        }
        is(RayState.filling) {
          buf.io.x := x
          buf.io.y := y
          buf.io.wEnable := true.B

          wall.io.x := x
          wall.io.y := y

          buf.io.dataIn := PriorityMux(
            Seq(
              wall.io.visible -> wall.io.color,
              (y > 120.U) -> 3.U,
              true.B -> 15.U
            )
          )

          when(xWrap && yWrap) { rayState := RayState.waiting }
        }
        is(RayState.waiting) {
          state := S.renderSprite1
          rayState := RayState.idle
        }
      }

    }
    is(S.renderSprite1) {
      sprite.io.input.valid := true.B
      sprite.io.input.bits.pos := posExchange.io.bobPos.bits
      sprite.io.input.bits.playerAngle := player.io.angle
      sprite.io.input.bits.playerPos := player.io.pos
      when(sprite.io.input.fire) {
        state := S.renderSprite2
      }
    }
    is(S.renderSprite2) {
      when(sprite.io.output.valid) {
        invSpritelen := sprite.io.output.bits.invLen
        bob.io.setPos.wrEn := true.B
        bob.io.scale := sprite.io.output.bits.invLen
          .fpMul(toFP(400))
          .asUInt(23, 11)
        bob.io.screen.x := circx
        bob.io.screen.y := circy
        bob.io.setPos.x := sprite.io.output.bits.xOffset
        bob.io.setPos.y := 120.U + sprite.io.output.bits.invLen
          .fpMul(toFP(8))
          .asUInt(23, 10)

        // TODO: clamp the x coordinate to the screen width
        buf.io.x := circx
        buf.io.y := circy
        buf.io.wEnable := sprite.io.output.bits.visible && bob.io.visible

        buf.io.dataIn := bob.io.pixel

        when(cxwrap && cywrap) {
          state := S.draw
          sprite.io.output.ready := true.B

        }

      }
    }

    is(S.draw) {
      // Draw the scene to the framebuffer
      buf.io.valid := true.B
      when(buf.io.newFrame) {
        state := S.idle
      }
    }

    is(S.win) {
      // Display win screen
      buf.io.x := winx
      buf.io.y := winy
      buf.io.wEnable := true.B
      buf.io.dataIn := 14.U // White color for the win screen
      when(wxwrap && wywrap) {
        buf.io.valid := true.B
        state := S.done
      }
    }
    is(S.done) { /*Do nothing*/ }
  }

  io.vga := buf.io.vga

}
