package gameEngine

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import scala.util.Random
import gameEngine.fixed.FixedPointUtils._
import gameEngine.vec2.Vec2
import gameEngine.raycast.Raycaster
import gameEngine.trig.TrigLUT

class RaycastDriver extends Module {
  val io = IO(new Bundle {
    val pos = Input(new Vec2(SInt(32.W)))
    val angle = Input(SInt(32.W))
    val valid = Input(Bool())
    val hitPos = Output(new Vec2(SInt(32.W)))
    val hitIdx = Output(new Vec2(UInt(16.W)))
    val hitTile = Output(Bool())
  })

  val _map = Seq(
    Seq(1, 1, 1, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 1, 1)
  )

  val map = VecInit.tabulate(4, 4) { (x, y) => _map(x)(y).B }
  for (x <- 0 to 3; y <- 0 to 3) {
    assert(map(x)(y) === _map(x)(y).B)
  }

  val raycaster = Module(new Raycaster)

  val trig = Module(new TrigLUT)
  trig.io.angle := io.angle

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  def near(a: SInt, b: SInt, tol: Double = 0.001): Bool = {
    val diff = a - b
    val absDiff = Mux(diff >= 0.S, diff, -diff)
    absDiff <= toFP(tol)
  }

  raycaster.io.in.valid := io.valid
  raycaster.io.in.bits.start := io.pos
  raycaster.io.in.bits.angle := io.angle

  val pos = raycaster.io.out.bits.pos
  val hitIdx = {

    val idxFP = Mux(
      raycaster.io.out.bits.isHorizontal,
      Mux(
        north,
        Vec2(pos.x.fpFloor, pos.y),
        Vec2(pos.x.fpFloor, pos.y - toFP(1.0))
      ),
      Mux(
        east,
        Vec2(pos.x, pos.y.fpFloor),
        Vec2(pos.x - toFP(1.0), pos.y.fpFloor)
      )
    )

    Vec2(idxFP.x(31, 16), idxFP.y(31, 16))

  }

  val tileHit = map(hitIdx.x(1, 0))(hitIdx.y(1, 0))

  raycaster.io.stop := tileHit
  raycaster.io.out.ready := false.B

  io.hitPos := pos
  io.hitIdx := hitIdx
  io.hitTile := tileHit

}

class RaycastDriverSpec extends AnyFunSpec with ChiselSim with Matchers {

  val testCases = Seq(
    ((1.5, 1.5), math.Pi / 6, (3, 2)),
    ((1.5, 1.5), math.Pi / 6 * 2, (2, 3)),
    ((1.5, 1.5), math.Pi / 8 * 7, (0, 1)),
    ((1.5, 1.5), math.Pi / 16 * 9, (1, 3)),
    ((1.5, 1.5), math.Pi / 6 * 7, (0, 1)),
    ((1.5, 1.5), math.Pi / 6 * 8, (1, 0)),
    ((1.5, 1.5), math.Pi / 6 * 8, (1, 0)),
    ((1.5, 1.5), math.Pi / 6 * 10, (1, 0)),
    ((1.5, 1.5), math.Pi / 6 * 11, (2, 0)),
    ((1.5, 1.5), 0.0, (3, 1)),
    ((1.5, 1.5), math.Pi / 2, (1, 3)),
    ((1.5, 1.5), math.Pi, (0, 1)),
    ((1.5, 1.5), math.Pi / 2 * 3, (1, 0))
  )

  describe("RaycastDriver") {

    it("pass general test cases") {
      for ((start, angle, hit) <- testCases) {

        withClue(f"${start} - $angle - $hit") {

          simulate(new RaycastDriver) { dut =>
            dut.io.pos.x.poke(toFP(start._1))
            dut.io.pos.y.poke(toFP(start._2))
            dut.io.angle.poke(toFP(angle))
            dut.io.valid.poke(true)
            dut.clock.step(10)

            dut.io.hitIdx.x.peek().litValue should be(hit._1)
            dut.io.hitIdx.y.peek().litValue should be(hit._2)
          }
        }
      }
    }

  }
}
