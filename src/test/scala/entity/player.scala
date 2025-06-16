package gameEngine.entity

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim

import gameEngine.entity.PlayerEntity
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.vec2.Vec2._
class PlayerEntitySpec extends AnyFunSpec with ChiselSim with Matchers {
  def printPlayer(
      playerPosX: Double,
      playerPosY: Double,
      map: Seq[Seq[Int]]
  ): String = {
    val playerX = playerPosX.toInt
    val playerY = playerPosY.toInt

    val mapStr = map.zipWithIndex.map { case (row, y) =>
      row.zipWithIndex
        .map { case (cell, x) =>
          if (x == playerX && y == playerY) "P"
          else if (cell == 1) "█"
          else " "
        }
        .mkString("")
    }

    mapStr.mkString("\n")
  }

  def getPlayerPos(
      player: PlayerEntity
  ): (Double, Double) = {
    val pos = player.io.pos
    (pos.x.peek().toDouble, pos.y.peek().toDouble)
  }

  describe("PlayerEntity") {
    it("should rotate correctly") {
      simulate(new PlayerEntity(0.0, 0.0, 0.0, Seq(Seq(1, 1, 1)))) { dut =>
        dut.io.action.poke(PlayerAction.turn)
        // mov 4/pi
        dut.io.actionArg.poke(toFP(math.Pi / 4.0))
        dut.clock.step(2)

        dut.io.angle.expect(toFP(math.Pi / 4.0))
        dut.clock.step()
        dut.io.angle.expect(toFP(math.Pi / 2.0))
        dut.clock.step()

        dut.io.angle.expect(toFP(3 * math.Pi / 4))
        dut.io.actionArg.poke(toFP(math.Pi))

        dut.clock.step()
        dut.io.angle.expect(toFP(math.Pi))
        dut.clock.step()
        dut.io.angle.expect(0) // wrap around to 0
        dut.clock.step()
        dut.io.angle.expect(toFP(math.Pi))
      }
    }
    it("should move around without turning") {
      val map = Seq(
        Seq(1, 1, 1, 1, 1),
        Seq(1, 0, 0, 0, 1),
        Seq(1, 0, 0, 0, 1),
        Seq(1, 0, 0, 0, 1),
        Seq(1, 1, 1, 1, 1)
      )
      simulate(new PlayerEntity(1.0, 1.0, 0.0, map)) { dut =>
        dut.io.action.poke(PlayerAction.moveForward)
        dut.io.actionArg.poke(toFP(1.0))
        dut.clock.step(2)

        dut.io.pos.x.expect(toFP(2.0))
        dut.io.pos.y.expect(toFP(1.0))
        dut.clock.step()
        dut.io.pos.x.expect(toFP(3.0))
        dut.io.pos.y.expect(toFP(1.0))
        // Collision should stop the player from moving further
        dut.clock.step()
        val (x, y) = getPlayerPos(dut)
        dut.io.pos.x.expect(toFP(3.0), printPlayer(x, y, map))
        dut.io.pos.y.expect(toFP(1.0))

        dut.io.action.poke(PlayerAction.moveBackward)

      }
    }

    it("should move around with turning") {
      val map = Seq(
        Seq(1, 1, 1, 1, 1),
        Seq(1, 0, 0, 0, 1),
        Seq(1, 0, 0, 0, 1),
        Seq(1, 0, 0, 0, 1),
        Seq(1, 1, 1, 1, 1)
      )
      simulate(new PlayerEntity(1.0, 1.0, 0.0, map)) { dut =>
        dut.io.action.poke(PlayerAction.turn)
        dut.io.actionArg.poke(toFP(math.Pi / 4.0)) // Turn 45 degrees
        dut.clock.step()
        dut.io.action.poke(PlayerAction.idle)
        dut.clock.step(2)
        dut.io.action.poke(PlayerAction.moveForward)
        dut.io.actionArg.poke(toFP(1.0)) // Move forward 1 unit
        dut.clock.step(2)

        dut.io.angle.expect(toFP(math.Pi / 4.0))
        val (x, y) = getPlayerPos(dut)
        print(s"Player position after moving: ($x, $y)\n")

      }
    }

  }
}
