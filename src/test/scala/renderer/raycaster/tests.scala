package gameEngine.raycast

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2._

class RaycasterSpec extends AnyFlatSpec with ChiselSim with Matchers {
  type Test = (Vec2D, Double, Int)

  "Raycaster" should "calculates correct position for general angles" in {
    val tests: Seq[Test] = Seq(
      // Shoot ray east
      (Vec2D(0.0, 0.0), 0, 3),

      // Shoot ray north east east
      (Vec2D(0.0, 0.0), math.Pi / 8, 0),
      (Vec2D(0.0, 0.0), math.Pi / 8, 2),
      (Vec2D(0.0, 0.0), math.Pi / 8, 3),
      (Vec2D(0.0, 0.0), math.Pi / 8, 4),

      // Shoot ray at north east
      (Vec2D(0.0, 0.0), math.Pi / 4, 0),
      (Vec2D(0.0, 0.0), math.Pi / 4, 1),
      (Vec2D(0.0, 0.0), math.Pi / 4, 2),
      (Vec2D(0.0, 0.0), math.Pi / 4, 3),
      (Vec2D(0.0, 0.0), math.Pi / 4, 4),

      // Shoot ray north
      (Vec2D(0.0, 0.0), math.Pi / 2, 0),
      (Vec2D(0.0, 0.0), math.Pi / 2, 1),
      (Vec2D(0.0, 0.0), math.Pi / 2, 2),
      (Vec2D(0.0, 0.0), math.Pi / 2, 3),

      // Shoot ray about north west
      (Vec2D(0.0, 0.0), 2.0, 0),
      (Vec2D(0.0, 0.0), 2.0, 2),
      (Vec2D(0.0, 0.0), 2.0, 3),
      (Vec2D(0.0, 0.0), 2.0, 4),

      // Shoot ray north west
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 0),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 1),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 2),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 3),
      (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 4),

      // Shoot ray west
      (Vec2D(0.0, 0.0), math.Pi, 0),
      (Vec2D(0.0, 0.0), math.Pi, 1),
      (Vec2D(0.0, 0.0), math.Pi, 2),
      (Vec2D(0.0, 0.0), math.Pi, 3),

      // Shoot ray south west
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 0),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 1),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 2),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 3),
      (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 4),

      // Shoot ray south
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 0),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 1),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 2),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 3),
      (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 4),

      // Shoot ray south east
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 0),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 1),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 2),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 3),
      (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 4),

      // Towards origin from grid
      (Vec2D(0.5, 0.0), math.Pi, 0),
      (Vec2D(0.0, 0.5), 3 * math.Pi / 2, 0),
      (Vec2D(-0.5, 0.0), 0, 0),
      (Vec2D(0.0, -0.5), math.Pi / 2, 0),

      // From middle of grid to right
      (Vec2D(0.5, 0.5), 0, 0),
      (Vec2D(0.5, 0.5), 0, 1),
      (Vec2D(0.5, 0.5), 0, 2),

      // Up
      (Vec2D(0.5, 0.5), math.Pi / 2, 0),
      (Vec2D(0.5, 0.5), math.Pi / 2, 1),
      (Vec2D(0.5, 0.5), math.Pi / 2, 2),

      // Down
      (Vec2D(0.5, 0.5), 3 * math.Pi / 2, 0),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 2, 1),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 2, 2),

      // Left
      (Vec2D(0.5, 0.5), math.Pi, 0),
      (Vec2D(0.5, 0.5), math.Pi, 1),
      (Vec2D(0.5, 0.5), math.Pi, 2),

      // NW
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 0),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 1),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 2),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 3),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 4),

      // SW
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 0),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 1),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 2),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 3),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 4),

      // SE
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 0),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 1),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 2),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 3),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 4),

      // A few random angles
      (Vec2D(-1.2, -3.4), 0.0, 1),
      (Vec2D(-1.2, -3.4), math.Pi / 2, 2),
      (Vec2D(-1.2, -3.4), math.Pi, 3),
      (Vec2D(-1.2, -3.4), 3 * math.Pi / 2, 4),
      (Vec2D(-1.2, -3.4), 2, 4),
      (Vec2D(0.2, 0.2), 0.1, 1),
      (Vec2D(0.8, 0.3), 1.47, 2),
      (Vec2D(-0.3, -0.7), 4 * math.Pi / 3, 1),
      (Vec2D(-0.3, -0.7), 4 * math.Pi / 3, 2),
      (Vec2D(0.3, 0.3), math.Pi / 4, 2),
      (Vec2D(0.0, 0.0), math.Pi / 4, 20)
    )

    val testsBySteps = tests.groupBy(_._3).toSeq.sortBy((_._1))
    val nTests = tests.length

    var maxErrCase: (Test, Double) = ((Vec2D(0.0, 0.0), 0, 0), 0.0)
    var errSum = 0.0

    testsBySteps.foreach { case (steps, testGroup) =>
      simulate(new Raycaster(steps)) { dut =>
        dut.io.stop.poke(false.B)
        testGroup.foreach { case (pos, angle, _) =>
          withClue(
            f"Test: [${pos.x}%.1f,${pos.y}%.1f] @ $angle%.3f rad x $steps"
          ) {
            dut.io.in.valid.poke(false.B)
            dut.io.out.ready.poke(false.B)

            dut.io.in.bits.start.x.poke(toFP(pos.x))
            dut.io.in.bits.start.y.poke(toFP(pos.y))
            dut.io.in.bits.angle.poke(toFP(angle))
            dut.clock.step()

            dut.io.in.ready
              .expect(true.B, "DUT should be ready to accept new calculations")

            // Start computation
            dut.io.in.valid.poke(true.B)
            dut.clock.step()
            dut.io.in.valid.poke(false.B)

            dut.io.in.ready
              .expect(
                false.B,
                "Backpressure should assert when starting new calculation"
              )

            dut.io.out.ready.poke(true.B)
            var seen = 0
            while (seen < steps + 1) {
              dut.clock.stepUntil(dut.io.out.valid, 1, (steps + 1) * 10)
              dut.clock.step()
              seen += 1
            }
            dut.io.out.ready.poke(false.B)

            // Validate outputs
            val got =
              Vec2D(
                dut.io.out.bits.pos.x.peek().toDouble,
                dut.io.out.bits.pos.y.peek().toDouble
              )

            val gotHitHorizontal =
              dut.io.out.bits.isHorizontal.peek().litToBoolean

            val exp =
              RaycasterGoldenModel.expectedDdaPos(pos, angle, steps)
            val err = math.abs(exp.norm - got.norm)
            errSum += err
            if (err > maxErrCase._2) {
              maxErrCase = ((pos, angle, steps), err)
            }
            exp.norm should be(got.norm +- 0.1)
          }
        }
      }
    }

    val test = maxErrCase._1
    val pos = test._1
    val angle = test._2
    val steps = test._3
    val err = maxErrCase._2
    info(
      f"Maximum error was: ${err}%.3f in case: [${pos.x}%.1f, ${pos.y}%.1f @ $angle%.3f rad x $steps]"
    )
    info(
      f"Average error was: ${errSum / nTests}%.3f"
    )
  }
  "Raycaster" should "determine if wall that was hit, was horizontal or vertical" in {

    type Test = (Vec2D, Double, Int, Boolean)
    val generalTests: Seq[Test] = Seq(
      (Vec2D(1.5, 1.5), 0.0, 0, false),
      (Vec2D(1.5, 1.5), math.Pi / 2, 1, true),
      (Vec2D(1.5, 1.5), math.Pi, 0, false),
      (Vec2D(1.5, 1.5), 3 * math.Pi / 2, 1, true),
      (Vec2D(1.5, 1.5), math.Pi / 6, 0, false),
      (Vec2D(1.5, 1.5), math.Pi / 3, 1, false),
      (Vec2D(1.5, 1.5), 7 * math.Pi / 6, 0, false),
      (Vec2D(1.5, 1.5), 4 * math.Pi / 3, 0, true)
    )

    val edgeCases: Seq[Test] = Seq(
      // At gridline, exact direction
      (Vec2D(0, 0), 0.0, 0, false),
      (Vec2D(0, 0), 0.0, 1, false),
      (Vec2D(0, 0), math.Pi / 2, 0, true),
      (Vec2D(0, 0), math.Pi / 2, 1, true),
      (Vec2D(0, 0), math.Pi, 0, false),
      (Vec2D(0, 0), math.Pi, 1, false),
      (Vec2D(0, 0), 3 * math.Pi / 2, 0, true),
      (Vec2D(0, 0), 3 * math.Pi / 2, 1, true),

      // at gridline, pointing towards corner
      (Vec2D(0, 0), math.Pi / 4, 0, false),
      (Vec2D(0, 0), math.Pi / 4, 1, true),
      (Vec2D(0, 0), math.Pi / 4, 2, false),
      (Vec2D(0, 0), math.Pi / 4, 3, true),
      //
      (Vec2D(0, 0), 3 * math.Pi / 4, 0, false),
      (Vec2D(0, 0), 3 * math.Pi / 4, 1, true),
      (Vec2D(0, 0), 3 * math.Pi / 4, 2, false),
      (Vec2D(0, 0), 3 * math.Pi / 4, 3, true),
      //
      (Vec2D(0, 0), 5 * math.Pi / 4, 0, false),
      (Vec2D(0, 0), 5 * math.Pi / 4, 1, true),
      (Vec2D(0, 0), 5 * math.Pi / 4, 2, false),
      (Vec2D(0, 0), 5 * math.Pi / 4, 3, true),
      //
      (Vec2D(0, 0), 7 * math.Pi / 4, 0, false),
      (Vec2D(0, 0), 7 * math.Pi / 4, 1, true),
      (Vec2D(0, 0), 7 * math.Pi / 4, 2, false),
      (Vec2D(0, 0), 7 * math.Pi / 4, 3, true)
    )

    val tests = (generalTests ++ edgeCases).distinct
    val testsBySteps = tests.groupBy(_._3).toSeq.sortBy((_._1))
    testsBySteps.foreach { case (steps, testGroup) =>
      simulate(new Raycaster(steps)) { dut =>
        dut.io.stop.poke(false.B)
        testGroup.foreach { case (pos, angle, _, exp) =>
          withClue(
            f"Test: [${pos.x}%.1f,${pos.y}%.1f] @ $angle%.3f rad x $steps: horizontal $exp"
          ) {
            dut.io.in.valid.poke(false.B)
            dut.io.out.ready.poke(false.B)

            dut.io.in.bits.start.x.poke(toFP(pos.x))
            dut.io.in.bits.start.y.poke(toFP(pos.y))
            dut.io.in.bits.angle.poke(toFP(angle))
            dut.clock.step()

            dut.io.in.ready
              .expect(true.B, "DUT should be ready to accept new calculations")

            // Start computation
            dut.io.in.valid.poke(true.B)
            dut.clock.step()
            dut.io.in.valid.poke(false.B)

            dut.io.in.ready
              .expect(
                false.B,
                "Backpressure should assert when starting new calculation"
              )

            var seen = 0
            dut.io.out.ready.poke(true.B)
            while (seen < steps + 1) {
              dut.clock.stepUntil(dut.io.out.valid, 1, (steps * 10 + 10))
              dut.clock.step()
              seen += 1
            }
            dut.io.out.ready.poke(false.B)

            // Validate outputs

            val gotHitHorizontal =
              dut.io.out.bits.isHorizontal.peek().litToBoolean

            gotHitHorizontal should be(exp)

          }
        }
      }
    }
  }

  "Raycaster" should "respect io.stop signal" in {
    type Test = (Vec2D, Double)

    val tests: Seq[Test] = Seq(
      (Vec2D(0.5, 0.5), 0)
    )

    simulate(new Raycaster(12)) { dut =>
      tests.foreach { case (pos, angle) =>
        withClue(
          f"Test: [${pos.x}%.1f,${pos.y}%.1f] @ $angle%.3f rad"
        ) {

          dut.io.stop.poke(false.B)
          dut.io.in.valid.poke(false.B)
          dut.io.out.ready.poke(false.B)

          dut.io.in.bits.start.x.poke(toFP(pos.x))
          dut.io.in.bits.start.y.poke(toFP(pos.y))
          dut.io.in.bits.angle.poke(toFP(angle))
          dut.clock.step()

          dut.io.in.ready
            .expect(true.B, "DUT should be ready to accept new calculations")

          // Start computation
          dut.io.in.valid.poke(true.B)
          dut.clock.step()
          dut.io.in.valid.poke(false.B)

          dut.clock.stepUntil(dut.io.out.valid, 1, 50)

          val exp = RaycasterGoldenModel.expectedDdaPos(pos, angle, 0)

        }
      }
    }
  }

}
