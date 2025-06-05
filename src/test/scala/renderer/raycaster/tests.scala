package gameEngine.raycast

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2._

class RaycasterSpec extends AnyFlatSpec with ChiselSim with Matchers {
  type Test = (Vec2D, Double, Int)

  "Raycaster" should "act according to ready/valid handshake, both on input and output" in {
    simulate(new Raycaster) { dut =>
      dut.io.in.ready
        .expect(true.B, "DUT should initially be ready to recieve requests")
      dut.io.out.valid
        .expect(false.B, "DUT shouldn't have data waiting on init")

      dut.io.stop.poke(false.B)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(false.B)

      dut.clock.step(10)
      dut.io.in.ready
        .expect(true.B, "DUT should still be ready to recieve requests")
      dut.io.out.valid
        .expect(false.B, "DUT still shouldn't have data waiting on init")

      dut.io.in.bits.start.x.poke(toFP(0.0))
      dut.io.in.bits.start.y.poke(toFP(0.0))
      dut.io.in.bits.angle.poke(toFP(math.Pi / 2))
      dut.io.in.valid.poke(true.B)
      dut.clock.step()

      dut.io.in.ready.expect(
        false.B,
        "DUT should now be calculating after receiving request"
      )

      // Immediatly get new data ready
      dut.io.in.bits.start.x.poke(toFP(2.0))
      dut.io.in.bits.start.y.poke(toFP(2.0))
      dut.io.in.bits.angle.poke(toFP(math.Pi))
      dut.io.in.valid.poke(true.B)

      for (i <- 1 until 4) {
        dut.clock.step()
        dut.io.out.valid
          .expect(false.B, f"Data shouldnt be ready yet after $i cycles")
        dut.io.in.ready.expect(
          false.B,
          f"DUT should still be calculating after $i cycles"
        )
      }

      dut.io.stop.poke(true.B)
      dut.clock.step()
      dut.io.stop.poke(false.B)

      dut.io.out.valid.expect(
        true.B,
        "DUT should have valid data on output after being stopped"
      )
      dut.io.in.ready.expect(
        false.B,
        "DUT shouldn't be ready serve, response hasn't been consumed yet"
      )

      math.sqrt(dut.io.out.bits.dist.peek().toDouble) should be(3.0 +- 0.05)

      dut.io.out.ready.poke(true.B)
      dut.clock.step()
      dut.io.out.ready.poke(false.B)

      dut.io.out.valid.expect(
        false.B,
        "DUT should move on and not indicate that data is waiting"
      )
      dut.io.in.ready.expect(true.B, "DUT should be ready to serve new request")
      dut.clock.step()

      dut.io.in.ready.expect(
        false.B,
        "Valid is still asserted, new request is immediatly processed"
      )
      dut.io.out.valid.expect(
        false.B,
        "DUT is not done"
      )
      dut.io.in.valid.poke(false.B)

      for (i <- 1 until 13) {
        dut.clock.step()
        dut.io.out.valid
          .expect(false.B, f"DUT shouldn't indicate valid on clock cycle $i")
        dut.io.in.ready
          .expect(false.B, f"DUT shouldn't be ready for more on clock cycle $i")

      }
      dut.clock.step()
      dut.io.out.valid.expect(true.B, "MaxSteps should have caught it now")
      dut.io.in.ready
        .expect(
          false.B,
          f"DUT shouldn't be ready yet, data hasn't been consumed yet"
        )

      math.sqrt(dut.io.out.bits.dist.peek().toDouble) should be(12.0 +- 0.05)

      for (_ <- 0 until 10) {
        dut.clock.step()
        dut.io.out.valid
          .expect(true.B, "Data isn't consumed, should still wait ")
        dut.io.in.ready
          .expect(
            false.B,
            f"DUT shouldn't be ready yet, data hasn't been consumed yet"
          )
      }

      dut.io.out.ready.poke(true.B)

      for (_ <- 0 until 10) {
        dut.clock.step()
        dut.io.out.valid.expect(false.B, "DUT shouldn't indicate awaiting data")
        dut.io.in.ready.expect(true.B, "DUT should be ready to consume data")
      }

    }
  }

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
        testGroup.foreach { case (pos, angle, _) =>
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

          dut.io.in.ready
            .expect(
              false.B,
              "Backpressure should assert when starting new calculation"
            )

          dut.clock.stepUntil(dut.io.out.valid, 1, (steps + 1))
          dut.io.out.valid.expect(true.B, "Ray should be finished calculating")

          dut.io.out.ready.poke(true.B)
          dut.clock.step()
          dut.io.out.ready.poke(false.B)

          // Validate outputs
          val got =
            Vec2D(
              dut.io.out.bits.pos.x.peek().toDouble,
              dut.io.out.bits.pos.y.peek().toDouble
            )
          val tol = 0.1

          val exp = RaycasterGoldenModel.expectedDdaPos(pos, angle, steps)
          val err = Vec2D(math.abs(got.x - exp.x), math.abs(got.y - exp.y)).norm
          errSum += err
          if (err > maxErrCase._2) {
            maxErrCase = ((pos, angle, steps), err)
          }
          assert(
            err < tol,
            f"Test failed: [${pos.x}%.1f,${pos.y}%.1f] @ $angle%.3f rad x $steps. Got: (${got.x}%.3f,${got.y}%.3f), expected: (${exp.x}%.3f, ${exp.y}%.3f)"
          )
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

}
