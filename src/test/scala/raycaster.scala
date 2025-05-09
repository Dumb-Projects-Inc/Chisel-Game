package gameEngine.screen.raycast

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import gameEngine.fixed.FixedPointUtils._
import org.scalatest.matchers.should.Matchers

case class Vec2D(x: Double, y: Double) {
  override def toString: String = s"(x=$x, y=$y)"
}

object Vec2D {
  def apply(x: Double, y: Double): Vec2D = {
    new Vec2D(x, y)
  }
}

class RaycasterSpec extends AnyFlatSpec with Matchers {

  def expectedDdaPos(
      start: Vec2D,
      angle: Double,
      nSteps: Int = 0
  ): Vec2D = {
    val pointsEast = angle < math.Pi / 2 || angle > 3 * math.Pi / 2
    val pointsNorth = angle < math.Pi

    val vertical = math.cos(angle) == 0
    val horizontal = math.sin(angle) == 0

    val tan = math.tan(angle)
    val cot = 1.0 / tan

    val hRay0 = {
      val y0 = if (pointsNorth) math.ceil(start.y) else math.floor(start.y)
      if (horizontal) {
        Vec2D(Double.PositiveInfinity, start.y)
      } else {
        Vec2D(start.x + (y0 - start.y) * cot, y0)
      }
    }

    val vRay0 = {
      val x0 = if (pointsEast) math.ceil(start.x) else math.floor(start.x)
      if (vertical) {
        Vec2D(start.x, Double.PositiveInfinity)
      } else {
        Vec2D(x0, start.y + (x0 - start.x) * tan)
      }
    }

    val hDx = {
      if (vertical) {
        0.0
      } else if (horizontal) {
        Double.PositiveInfinity
      } else {
        if (pointsNorth) cot else -cot
      }
    }
    val hDy = if (pointsNorth) 1.0 else -1.0

    val vDx = if (pointsEast) 1.0 else -1.0
    val vDy = {
      if (vertical) {
        Double.PositiveInfinity
      } else if (horizontal) {
        0.0
      } else {
        if (pointsEast) tan else -tan
      }
    }

    var hRay = hRay0
    var vRay = vRay0

    def hRayLen() = {
      math.sqrt(math.pow(hRay.x - start.x, 2) + math.pow(hRay.y - start.y, 2))
    }

    def vRayLen()= {
      math.sqrt(math.pow(vRay.x - start.x, 2) + math.pow(vRay.y - start.y, 2))
    }

    var pos = if (hRayLen() <= vRayLen()) hRay else vRay

    for (_ <- 0 until nSteps) {
      if (hRayLen() < vRayLen()) {
        hRay = Vec2D(
          hRay.x + hDx,
          hRay.y + hDy
        )
      } else if (vRayLen() < hRayLen()) {
        vRay = Vec2D(
          vRay.x + vDx,
          vRay.y + vDy
        )
      } else {
        hRay = Vec2D(
          hRay.x + hDx,
          hRay.y + hDy
        )
        vRay = Vec2D(
          vRay.x + vDx,
          vRay.y + vDy
        )
      }

      if (hRayLen() <= vRayLen()) {
        pos = hRay
      } else {
        pos = vRay
      }
    }

    (pos)
  }

  behavior of "expectedDdaPos"
  it should "implement golden model correctly" in {
    val origoTests: Seq[(Vec2D, Double, Int, Vec2D)] =
      Seq(
        // Shoot ray horizontally
        (Vec2D(0.0, 0.0), 0, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 0, 1, Vec2D(1.0, 0.0)),
        (Vec2D(0.0, 0.0), 0, 2, Vec2D(2.0, 0.0)),
        (Vec2D(0.0, 0.0), 0, 3, Vec2D(3.0, 0.0)),

        // Shoot ray at 22.5 deg
        (Vec2D(0.0, 0.0), math.Pi / 8, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 1, Vec2D(1.0, 0.4142135)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 2, Vec2D(2.0, 0.8284271)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 3, Vec2D(2.4142135, 1)),

        // Shoot ray at 45 deg
        (Vec2D(0.0, 0.0), math.Pi / 4, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 4, 1, Vec2D(1.0, 1.0)),
        (Vec2D(0.0, 0.0), math.Pi / 4, 2, Vec2D(2.0, 2.0)),
        (Vec2D(0.0, 0.0), math.Pi / 4, 3, Vec2D(3.0, 3.0)),

        // Shoot ray vertically
        (Vec2D(0.0, 0.0), math.Pi / 2, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 2, 1, Vec2D(0.0, 1.0)),
        (Vec2D(0.0, 0.0), math.Pi / 2, 2, Vec2D(0.0, 2.0)),
        (Vec2D(0.0, 0.0), math.Pi / 2, 3, Vec2D(0.0, 3.0))
      )

    val tests = origoTests.distinct
    for ((start, angle, steps, end) <- tests) {
      val result = expectedDdaPos(start, angle, steps)
      withClue(
        f"start: ${start} - angle: ${angle}, - steps: ${steps} - coord: X ="
      ) {
        result.x should be (end.x +- 1e-5)
      }
      withClue(
        f"start: ${start} - angle: ${angle}, - steps: ${steps} - coord: Y ="
      ) {
        result.y should be (end.y +- 1e-5)
      }
    }
  }
}
