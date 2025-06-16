package gameEngine.raycast

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2._

class RaycasterDriverSpec extends AnyFunSpec with ChiselSim with Matchers {

  case class Test(
      pos: Vec2D,
      angle: Double,
      fov: Double,
      nRays: Int,
      distances: Seq[Double]
  )

  val testCases: Seq[Test] = Seq(
    Test(
      pos = Vec2D(1.5, 1.5),
      angle = math.Pi / 4,
      fov = 2,
      nRays = 3,
      distances = Seq(1.522, 2.121, 1.525)
    ),
    Test(
      pos = Vec2D(1.1, 1.1),
      angle = 0,
      fov = 2,
      nRays = 6,
      distances = Seq(0.12, 0.176, 0.5, 1.94, 2.3, 2.25)
    ),
    Test(
      pos = Vec2D(1.5, 1.5),
      angle = 0.0,
      fov = 0.0,
      nRays = 1,
      distances = Seq(1.5)
    ),
    Test(
      pos = Vec2D(1.5, 1.5),
      angle = math.Pi / 2,
      fov = 0.0,
      nRays = 1,
      distances = Seq(1.5)
    ),
    Test(
      pos = Vec2D(1.5, 1.5),
      angle = math.Pi,
      fov = 0.0,
      nRays = 1,
      distances = Seq(0.5)
    ),
    Test(
      pos = Vec2D(1.5, 1.5),
      angle = math.Pi / 2 * 3,
      fov = 0.0,
      nRays = 1,
      distances = Seq(0.5)
    )
  )

  describe("RaycasterDriver") {
    it("pass general test cases") {
      for (t <- testCases) {
        simulate(new RaycastDriver(fov = t.fov, nRays = t.nRays, enableFovCorrection = false)) { dut =>
          withClue(t.toString() + "\n") {
            dut.io.request.ready.expect(true.B)
            dut.io.request.bits.angle.poke(toFP(t.angle))
            dut.io.request.bits.start.x.poke(toFP(t.pos.x))
            dut.io.request.bits.start.y.poke(toFP(t.pos.y))
            dut.io.request.valid.poke(true.B)
            dut.clock.step()
            dut.io.request.valid.poke(false.B)
            for (expDistance <- t.distances) {
              withClue(expDistance) {
                dut.io.response.ready.poke(false.B)
                dut.io.request.ready.expect(false.B)

                dut.clock.stepUntil(dut.io.response.valid, 1, 100)
                dut.io.response.valid.expect(true.B)
                math.sqrt(dut.io.response.bits.dist.peek().toDouble) should be(
                  expDistance +- 0.1
                )
                dut.io.response.bits.tile.peek().litValue should be(1)
                dut.io.response.ready.poke(true.B)
                dut.clock.step()
              }
            }
            dut.io.request.ready.peek().litValue should be(1)
          }
        }
      }
    }
  }
}
