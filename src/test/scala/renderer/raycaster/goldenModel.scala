package gameEngine.raycast

import org.scalatest.flatspec.AnyFlatSpec
import gameEngine.fixed.FixedPointUtils._
import org.scalatest.matchers.should.Matchers

case class Vec2D(x: Double, y: Double) {
  override def toString: String = s"(x=$x, y=$y)"

  def norm: Double = math.sqrt(x * x + y * y)
  def -(other: Vec2D) = Vec2D(x - other.x, y - other.y)
}

object Vec2D {
  def apply(x: Double, y: Double): Vec2D = {
    new Vec2D(x, y)
  }
}

object RaycasterGoldenModel {
  def expectedDdaPos(
      start: Vec2D,
      angle: Double,
      nSteps: Int = 0
  ): (Vec2D, Double) = {
    val pointsEast = math.cos(angle) > 0
    val pointsNorth = math.sin(angle) > 0

    val eps = 1e-6
    val vertical = math.abs(math.cos(angle)) < eps
    val horizontal = math.abs(math.sin(angle)) < eps

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
      math.pow(hRay.x - start.x, 2) + math.pow(hRay.y - start.y, 2)
    }

    def vRayLen() = {
      math.pow(vRay.x - start.x, 2) + math.pow(vRay.y - start.y, 2)
    }

    var pos = if (hRayLen() < vRayLen()) hRay else vRay

    for (_ <- 0 until nSteps) {
      if (hRayLen() < vRayLen()) {
        hRay = Vec2D(
          hRay.x + hDx,
          hRay.y + hDy
        )
      } else {
        vRay = Vec2D(
          vRay.x + vDx,
          vRay.y + vDy
        )
      }

      if (hRayLen() < vRayLen()) {
        pos = hRay
      } else {
        pos = vRay
      }
    }

    (pos, (pos - start).norm)
  }

}

class RaycasterGoldenModelSpec extends AnyFlatSpec with Matchers {
  import RaycasterGoldenModel._
  type Test = (Vec2D, Double, Int, Vec2D)

  def testAngels(tests: Seq[Test]): Unit = {
    for ((start, angle, steps, end) <- tests) {
      val (result, dist) = expectedDdaPos(start, angle, steps)
      withClue(
        f"start: ${start} - angle: ${angle}%.3f, - steps: ${steps} - coord: X ="
      ) {
        result.x should be(end.x +- 1e-5)
      }
      withClue(
        f"start: ${start} - angle: ${angle}%.3f, - steps: ${steps} - coord: Y ="
      ) {
        result.y should be(end.y +- 1e-5)
      }
    }
  }

  behavior of "expectedDdaPos"
  it should "handle rays cast from a origo correctly" in {
    val tests: Seq[Test] =
      Seq(
        // Shoot ray east
        (Vec2D(0.0, 0.0), 0, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 0, 1, Vec2D(1.0, 0.0)),
        (Vec2D(0.0, 0.0), 0, 2, Vec2D(2.0, 0.0)),
        (Vec2D(0.0, 0.0), 0, 3, Vec2D(3.0, 0.0)),

        // Shoot ray north east east
        (Vec2D(0.0, 0.0), math.Pi / 8, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 1, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 2, Vec2D(1.0, 0.4142135)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 3, Vec2D(2.0, 0.8284271)),
        (Vec2D(0.0, 0.0), math.Pi / 8, 4, Vec2D(2.4142135, 1)),

        // Shoot ray at north east
        // Ray hits both horizontal and vertical line at diagonal
        (Vec2D(0.0, 0.0), math.Pi / 4, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 4, 2, Vec2D(1.0, 1.0)),
        (Vec2D(0.0, 0.0), math.Pi / 4, 4, Vec2D(2.0, 2.0)),
        (Vec2D(0.0, 0.0), math.Pi / 4, 6, Vec2D(3.0, 3.0)),

        // Shoot ray north
        (Vec2D(0.0, 0.0), math.Pi / 2, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi / 2, 1, Vec2D(0.0, 1.0)),
        (Vec2D(0.0, 0.0), math.Pi / 2, 2, Vec2D(0.0, 2.0)),
        (Vec2D(0.0, 0.0), math.Pi / 2, 3, Vec2D(0.0, 3.0)),

        // Shoot ray about north west
        (Vec2D(0.0, 0.0), 2.0, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 2.0, 2, Vec2D(-0.45765, 1.0)),
        (Vec2D(0.0, 0.0), 2.0, 3, Vec2D(-0.91531, 2.0)),
        (Vec2D(0.0, 0.0), 2.0, 4, Vec2D(-1.0, 2.185039)),

        // Shoot ray north west
        (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 2, Vec2D(-1.0, 1.0)),
        (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 4, Vec2D(-2.0, 2.0)),
        (Vec2D(0.0, 0.0), 3 * math.Pi / 4, 6, Vec2D(-3.0, 3.0)),

        // Shoot ray west
        (Vec2D(0.0, 0.0), math.Pi, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi, 1, Vec2D(-1.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi, 2, Vec2D(-2.0, 0.0)),
        (Vec2D(0.0, 0.0), math.Pi, 3, Vec2D(-3.0, 0.0)),

        // Shoot ray south west
        (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 2, Vec2D(-1.0, -1.0)),
        (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 4, Vec2D(-2.0, -2.0)),
        (Vec2D(0.0, 0.0), 5 * math.Pi / 4, 6, Vec2D(-3.0, -3.0)),

        // Shoot ray south
        (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 1, Vec2D(0.0, -1.0)),
        (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 2, Vec2D(0.0, -2.0)),
        (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 3, Vec2D(0.0, -3.0)),
        (Vec2D(0.0, 0.0), 6 * math.Pi / 4, 4, Vec2D(0.0, -4.0)),

        // Shoot ray south east
        (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
        (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 2, Vec2D(1.0, -1.0)),
        (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 4, Vec2D(2.0, -2.0)),
        (Vec2D(0.0, 0.0), 7 * math.Pi / 4, 6, Vec2D(3.0, -3.0))
      )

    testAngels(tests)
  }

  it should "handle rays cast from non integer position" in {
    val tests: Seq[Test] = Seq(
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
      (Vec2D(0.5, 0.5), math.Pi / 4, 0, Vec2D(1.0, 1.0)),
      (Vec2D(0.5, 0.5), math.Pi / 4, 2, Vec2D(2.0, 2.0)),
      (Vec2D(0.5, 0.5), math.Pi / 4, 4, Vec2D(3.0, 3.0)),

      // NW
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 0, Vec2D(0.0, 1.0)),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 2, Vec2D(-1.0, 2.0)),
      (Vec2D(0.5, 0.5), 3 * math.Pi / 4, 4, Vec2D(-2.0, 3.0)),

      // SW
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 2, Vec2D(-1.0, -1.0)),
      (Vec2D(0.5, 0.5), 5 * math.Pi / 4, 4, Vec2D(-2.0, -2.0)),

      // SE
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 0, Vec2D(1.0, 0.0)),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 2, Vec2D(2.0, -1.0)),
      (Vec2D(0.5, 0.5), 7 * math.Pi / 4, 4, Vec2D(3.0, -2.0))
    )

    testAngels(tests)
  }

  it should "handle rays cast from negative start positions" in {
    val tests: Seq[Test] = Seq(
      (Vec2D(-1.2, -3.4), 0.0, 1, Vec2D(0.0, -3.4)),
      (Vec2D(-1.2, -3.4), math.Pi / 2, 2, Vec2D(-1.2, -1.0)),
      (Vec2D(-1.2, -3.4), math.Pi, 3, Vec2D(-5.0, -3.4)),
      (Vec2D(-1.2, -3.4), 3 * math.Pi / 2, 4, Vec2D(-1.2, -8)),
      (Vec2D(-1.2, -3.4), 2, 4, Vec2D(-2.7560356, 0))
    )
    testAngels(tests)
  }

  it should "handle angles outside [0,2pi)" in {
    val tests: Seq[Test] = Seq(
      // 2pi
      (Vec2D(0.0, 0.0), 2 * math.Pi, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 2 * math.Pi, 1, Vec2D(1.0, 0.0)),
      (Vec2D(0.0, 0.0), 2 * math.Pi, 2, Vec2D(2.0, 0.0)),
      (Vec2D(0.5, 0.0), 2 * math.Pi, 0, Vec2D(1.0, 0.0)),

      // negative
      (Vec2D(-0.2, 0.1), -3, 0, Vec2D(-0.901525, 0.0)),
      (Vec2D(0.0, 0.0), -2 * math.Pi / 4, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), -2 * math.Pi / 4, 1, Vec2D(-1.0, 0.0)),

      // large
      (Vec2D(0.0, 0.0), 10, 0, Vec2D(0.0, 0.0)),
      (Vec2D(0.0, 0.0), 10, 1, Vec2D(-1.0, -0.648))
    )
  }
}
