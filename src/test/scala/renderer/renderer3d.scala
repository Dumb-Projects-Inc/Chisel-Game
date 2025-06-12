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
import gameEngine.raycast._
import chisel3.util._

class RaycastDriver(fov: Double, nRays: Int) extends Module {
  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new RayRequest))
    val response = Decoupled(new RayResponse)
  })

  def near(a: SInt, b: SInt, tol: Double = 0.001): Bool = {
    val diff = a - b
    val absDiff = Mux(diff >= 0.S, diff, -diff)
    absDiff <= toFP(tol)
  }

  val halfFov: Double = fov / 2.0
  val step: Double = fov / (nRays - 1)
  val offsets = for (i <- 0 until nRays) yield (step * i - halfFov)
  val offsetsVec = VecInit.tabulate(nRays) { (i) => (toFP(offsets(i))) }

  val _map = Seq(
    Seq(1, 1, 1, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 1, 1)
  )

  val map = VecInit.tabulate(4, 4) { (x, y) => _map(x)(y).U }

  val raycaster = Module(new Raycaster)
  val queue = Module(new Queue(new RayResponse, 4))
  io.response <> queue.io.deq

  val trig = Module(new TrigLUT)
  val angleReg = RegInit(0.S(24.W))
  trig.io.angle := angleReg

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  val posReg = RegInit(Vec2(0.S(24.W), 0.S(24.W)))

  val currentRayOffsetIdx = RegInit(0.U(log2Ceil(nRays).W))
  val currentRayPos = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val currentRayHorizontal = RegInit(Bool())

  object S extends ChiselEnum {
    val idle, initRay, run, check = Value
  }
  val state = RegInit(S.idle)

  io.request.ready := false.B
  switch(state) {
    is(S.idle) {
      io.request.ready := true.B

      when(io.request.fire) {
        angleReg := io.request.bits.angle
        posReg := io.request.bits.start
        currentRayOffsetIdx := 0.U
        state := S.initRay
      }
    }
    is(S.initRay) {
      raycaster.io.in.bits.start := posReg
      raycaster.io.in.bits.angle := angleReg + offsetsVec(currentRayOffsetIdx)
      raycaster.io.in.valid := true.B
      when(raycaster.io.in.ready) {
        state := S.run
      }
    }
    is(S.run) {
      currentRayPos
    }
    is(S.check) {
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
        Vec2(idxFP.x(23, 12), idxFP.y(23, 12))
      }
      val tileHit = map(hitIdx.x(1, 0))(hitIdx.y(1, 0))
      raycaster.io.stop := tileHit === 0.U

      raycaster.io.out.ready := queue.io.enq.ready

      when(raycaster.io.out.valid) {}

    }
  }

  val pos = raycaster.io.out.bits.pos

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
            dut.clock.step(2)
            dut.io.valid.poke(true)
            dut.clock.step(25)

            dut.io.hitIdx.x.peek().litValue should be(hit._1)
            dut.io.hitIdx.y.peek().litValue should be(hit._2)
          }
        }
      }
    }

  }
}
