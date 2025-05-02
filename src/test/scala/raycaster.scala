package gameEngine.screen.raycast

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.FixedPointUtils._

class RaycasterSpec extends AnyFlatSpec {

  def expectedDdaPos(
      startX: Double,
      startY: Double,
      angle: Double,
      nSteps: Int = 0
  ): (Double, Double) = {
    val pointsEast = angle <= math.Pi / 2 || angle > 3 * math.Pi / 2
    val pointsSouth = angle < math.Pi

    val tan = math.tan(angle)
    val cot = 1.0 / tan

    val y0 = if (pointsSouth) math.ceil(startY) else math.floor(startY)
    val hRayX0 = startX + (y0 - startY) * cot
    val hRayY0 = y0

    val x0 = if (pointsEast) math.ceil(startX) else math.floor(startX)
    val vRayX0 = x0
    val vRayY0 = startY + (x0 - startX) * tan

    val hDx = if (pointsSouth) cot else -cot
    val hDy = if (pointsSouth) 1.0 else -1.0
    val vDx = if (pointsEast) 1.0 else -1.0
    val vDy = if (pointsEast) tan else -tan

    var hRayX = hRayX0
    var hRayY = hRayY0
    var vRayX = vRayX0
    var vRayY = vRayY0

    def pick() = {
      val dH2 =
        (hRayX - startX) * (hRayX - startX) + (hRayY - startY) * (hRayY - startY)
      val dV2 =
        (vRayX - startX) * (vRayX - startX) + (vRayY - startY) * (vRayY - startY)
      if (dH2 <= dV2) (hRayX, hRayY) else (vRayX, vRayY)
    }

    var (posX, posY) = pick()

    for (_ <- 1 to nSteps) {
      val dH2 =
        (hRayX - startX) * (hRayX - startX) + (hRayY - startY) * (hRayY - startY)
      val dV2 =
        (vRayX - startX) * (vRayX - startX) + (vRayY - startY) * (vRayY - startY)
      if (dH2 <= dV2) {
        hRayX += hDx
        hRayY += hDy
      } else {
        vRayX += vDx
        vRayY += vDy
      }
      val (x, y) = pick()
      posX = x; posY = y
    }

    (posX, posY)
  }

  behavior of "Raycaster"

  it should "calculate correct intersection" in {
    val positions = Seq(
      (0.01, 0.01),
      (0.5, 0.5),
      (0.0, 0.5),
      (0.5, 0.0),
      (1.0, 1.0),
      (0.0, 1.0),
      (1.0, 0.0)
    )

    val angles = Seq(
      0.05,
      math.Pi / 8,
      math.Pi / 4,
      math.Pi / 3,
      math.Pi / 2 + 0.05,
      3 * math.Pi / 4,
      math.Pi,
      5 * math.Pi / 4,
      3 * math.Pi / 2,
      7 * math.Pi / 4
    )
    val stepLimits = Seq(1, 4, 8)

    for (maxStep <- stepLimits) {

      simulate(new Raycaster(maxStep)) { dut =>
        for (pos <- positions; angle <- angles) {
          val (x, y) = pos
          dut.io.valid.poke(false.B)
          dut.io.mapTile.poke(false.B)
          dut.io.rayStart.x.poke(toFP(x))
          dut.io.rayStart.y.poke(toFP(y))
          dut.io.rayAngle.poke(toFP(angle))
          dut.clock.step()

          dut.io.ready.expect(true.B)

          dut.io.valid.poke(true.B)
          dut.clock.step()
          dut.io.valid.poke(false.B)
          dut.io.ready.expect(false.B)

          var counter = 0
          dut.clock.stepUntil(dut.io.ready, 1, maxStep * 10)

          val (expX, expY) = expectedDdaPos(x, y, angle, maxStep)
          val gotX = dut.io.pos.x.peek().toDouble
          val gotY = dut.io.pos.y.peek().toDouble

          val tol = 0.075
          val errX = math.abs(gotX - expX)

          val errY = math.abs(gotY - expY)
          if (errX >= tol) {
            info(
              f"WARNING: [$x,$y $angle,$maxStep] X off by $errX%.3f " +
                f"(expected $expX%.3f, got $gotX%.3f)"
            )
          }
          if (errY >= tol) {
            info(
              f"WARNING: [${x},${y} $angle,$maxStep] Y off by $errY%.3f (exp=$expY%.3f got=$gotY%.3f)"
            )
          }

        }
      }
    }

  }

  it should "assert ready in idle, drop ready after init, then reassert upon a complete calculation" in {
    simulate(new Raycaster(maxSteps = 8)) { dut =>
      // init
      dut.io.valid.poke(false.B)
      dut.io.mapTile.poke(false.B)
      dut.io.rayStart.x.poke(toFP(0.5))
      dut.io.rayStart.y.poke(toFP(0.5))
      dut.io.rayAngle.poke(toFP(math.Pi / 3))

      dut.clock.step()
      dut.io.ready.expect(
        true.B,
        "After reset and setup, ready should be high (idle and waiting for valid)"
      )

      // Load
      dut.io.valid.poke(true)
      dut.clock.step()
      dut.io.ready.expect(
        false.B,
        "Once we assert valid, ready must drop as the ray-cast begins. Should be in sInit"
      )

      // Force wall hit
      dut.io.mapTile.poke(true)
      dut.clock.step()
      dut.io.ready.expect(false.B, "Still processing, should be in sLoad")
      dut.clock.step()
      dut.io.ready.expect(false.B, "Still processing, should be in sCheck")
      dut.clock.step()
      dut.io.ready.expect(
        true.B,
        "sCheck found that wall was hit and sent us back to idle"
      )

      val (expX, expY) = expectedDdaPos(0.5, 0.5, math.Pi / 3)
      val gotX = dut.io.pos.x.peek().toDouble
      val gotY = dut.io.pos.y.peek().toDouble
      assert(
        math.abs(gotX - expX) < 1e-2,
        f"For x of initial intersection expected ${expX}, got ${gotX}"
      )
      assert(
        math.abs(gotY - expY) < 1e-2,
        f"For y of initial intersection expected ${expY}, got ${gotY}"
      )

    }
  }

}
