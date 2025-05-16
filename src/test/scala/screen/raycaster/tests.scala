package gameEngine.screen.raycast

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import gameEngine.fixed.FixedPointUtils._

class RaycasterSpec extends AnyFlatSpec with ChiselSim {
  type Test = (Vec2D, Double, Int, Vec2D)

  "Raycaster" should "calculates correct position for general angles" in {
    val tests: Seq[Test] = Seq(
      // Shoot ray east
      (Vec2D(0.0, 0.0), 0, 3, Vec2D(3.0, 0.0)),

      // Shoot ray north east east
      (Vec2D(0.0, 0.0), math.Pi / 8, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), math.Pi / 8, 1, Vec2D(1.0, 0.4142135)),
      (Vec2D(0.0, 0.0), math.Pi / 8, 2, Vec2D(2.0, 0.8284271)),
      (Vec2D(0.0, 0.0), math.Pi / 8, 3, Vec2D(2.4142135, 1)),

      // Shoot ray at north east
      // Ray hits both horizontal and vertical line at diagonal
      (Vec2D(0.0, 0.0), math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), math.Pi / 4, 1, Vec2D(1.0, 1.0)),
      (Vec2D(0.0, 0.0), math.Pi / 4, 2, Vec2D(2.0, 2.0)),
      (Vec2D(0.0, 0.0), math.Pi / 4, 3, Vec2D(3.0, 3.0)),

      // Shoot ray north
      (Vec2D(0.0, 0.0), math.Pi / 2, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), math.Pi / 2, 1, Vec2D(0.0, 1.0)),
      (Vec2D(0.0, 0.0), math.Pi / 2, 2, Vec2D(0.0, 2.0)),
      (Vec2D(0.0, 0.0), math.Pi / 2, 3, Vec2D(0.0, 3.0)),

      // Shoot ray about north west
      (Vec2D(0.0, 0.0), 2.0, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 2.0, 1, Vec2D(-0.45765, 1.0)),
      (Vec2D(0.0, 0.0), 2.0, 2, Vec2D(-0.91531, 2.0)),
      (Vec2D(0.0, 0.0), 2.0, 3, Vec2D(-1.0, 2.185039)),

      // Shoot ray north west
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 1, Vec2D(-1.0, 1.0)),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 2, Vec2D(-2.0, 2.0)),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 3, Vec2D(-3.0, 3.0)),

      // Shoot ray west
      (Vec2D(0.0, 0.0), math.Pi, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), math.Pi, 1, Vec2D(-1.0, 0.0)),
      (Vec2D(0.0, 0.0), math.Pi, 2, Vec2D(-2.0, 0.0)),
      (Vec2D(0.0, 0.0), math.Pi, 3, Vec2D(-3.0, 0.0)),

      // Shoot ray south west
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 1, Vec2D(-1.0, -1.0)),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 2, Vec2D(-2.0, -2.0)),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 3, Vec2D(-3.0, -3.0)),

      // Shoot ray south
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 1, Vec2D(0.0, -1.0)),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 2, Vec2D(0.0, -2.0)),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 3, Vec2D(0.0, -3.0)),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 4, Vec2D(0.0, -4.0)),

      // Shoot ray south east
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 1, Vec2D(1.0, -1.0)),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 2, Vec2D(2.0, -2.0)),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 3, Vec2D(3.0, -3.0)),

      // Towards origo from grid
      (Vec2D(0.5, 0.0), math.Pi, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.5), 3 * math.Pi / 2, 0, Vec2D(0.0, 0.0)),
      (Vec2D(-0.5, 0.0), 0, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, -0.5), math.Pi / 2, 0, Vec2D(0.0, 0.0)),

      // From middle of grid to right
      (Vec2D(0.5, 0.5), 0, 0, Vec2D(1.0, 0.5)),
      (Vec2D(0.5, 0.5), 0, 1, Vec2D(2.0, 0.5)),
      (Vec2D(0.5, 0.5), 0, 2, Vec2D(3.0, 0.5)),

      // Up
      (Vec2D(0.5, 0.5), math.Pi / 2, 0, Vec2D(0.5, 1.0)),
      (Vec2D(0.5, 0.5), math.Pi / 2, 1, Vec2D(0.5, 2.0)),
      (Vec2D(0.5, 0.5), math.Pi / 2, 2, Vec2D(0.5, 3.0)),

      // Down
      (Vec2D(0.5, 0.5), 3 * math.Pi / 2, 0, Vec2D(0.5, 0.0)),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 2, 1, Vec2D(0.5, -1.0)),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 2, 2, Vec2D(0.5, -2.0)),

      // Left
      (Vec2D(0.5, 0.5), math.Pi, 0, Vec2D(0.0, 0.5)),
      (Vec2D(0.5, 0.5), math.Pi, 1, Vec2D(-1.0, 0.5)),
      (Vec2D(0.5, 0.5), math.Pi, 2, Vec2D(-2.0, 0.5)),
      (Vec2D(0.5, 0.5), math.Pi / 4, 1, Vec2D(2.0, 2.0)),
      (Vec2D(0.5, 0.5), math.Pi / 4, 2, Vec2D(3.0, 3.0)),

      // NW
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 0, Vec2D(0.0, 1.0)),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 1, Vec2D(-1.0, 2.0)),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 2, Vec2D(-2.0, 3.0)),

      // SW
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 1, Vec2D(-1.0, -1.0)),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 2, Vec2D(-2.0, -2.0)),

      // SE
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 0, Vec2D(1.0, 0.0)),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 1, Vec2D(2.0, -1.0)),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 2, Vec2D(3.0, -2.0)),
      (Vec2D(-1.2, -3.4), 0.0, 1, Vec2D(0.0, -3.4)),
      (Vec2D(-1.2, -3.4), math.Pi / 2, 2, Vec2D(-1.2, -1.0)),
      (Vec2D(-1.2, -3.4), math.Pi, 3, Vec2D(-5.0, -3.4)),
      (Vec2D(-1.2, -3.4), 3 * math.Pi / 2, 4, Vec2D(-1.2, -8)),
      (Vec2D(-1.2, -3.4), 2, 4, Vec2D(-2.7560356, 0))
    )

    val testss = Seq(
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 2, Vec2D(-2.0, 3.0))
    )

    val testsBySteps = tests.groupBy(_._3)

    testsBySteps.foreach { case (steps, testGroup) =>
      simulate(new Raycaster(steps)) { dut =>
        testGroup.foreach { case (pos, angle, _, exp) =>
          dut.io.valid.poke(false.B)
          dut.io.stop.poke(false.B)
          dut.io.rayStart.x.poke(toFP(pos.x))
          dut.io.rayStart.y.poke(toFP(pos.y))
          dut.io.rayAngle.poke(toFP(angle))
          dut.clock.step()
          dut.io.ready.expect(true.B)

          // Start computation
          dut.io.valid.poke(true.B)
          dut.clock.step()
          dut.io.valid.poke(false.B)
          dut.io.ready.expect(false.B)

          dut.clock.stepUntil(dut.io.ready, 1, (steps + 1) * 5)
          dut.io.ready.expect(true.B)

          // Validate outputs
          val gotX = dut.io.pos.x.peek().toDouble
          val gotY = dut.io.pos.y.peek().toDouble
          val tol = 0.075
          assert(
            math.abs(gotX - exp.x) < tol && math.abs(gotY - exp.y) < tol,
            f"Test failed: [${pos.x}%.1f,${pos.y}%.1f] @ $angle%.3f rad, steps=$steps. Got: (${gotX}%.3f,${gotY}%.3f), expected: (${exp.x}%.3f, ${exp.y}%.3f)"
          )
        }
      }
    }
  }

}
