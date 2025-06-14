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
import chisel3.experimental.BundleLiterals._

class RayHit(nTiles: Int) extends Bundle {
  val dist = SInt(24.W)
  val tile = UInt(log2Ceil(nTiles).W)
}

class RaycastDriver(fov: Double = 2, nRays: Int = 12, nTiles: Int = 2)
    extends Module {
  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new RayRequest))
    val response = Decoupled(new RayHit(nTiles))
  })

  def near(a: SInt, b: SInt, tol: Double = 0.001): Bool = {
    val diff = a - b
    val absDiff = Mux(diff >= 0.S, diff, -diff)
    absDiff <= toFP(tol)
  }

  val offsets: Seq[Double] =
    if (nRays > 1) {
      val halfFov = fov / 2.0
      val step = fov / (nRays - 1)
      (0 until nRays).map(i => step * i - halfFov)
    } else {
      Seq(0.0)
    }
  val offsetsVec = VecInit.tabulate(nRays) { (i) => (toFP(offsets(i))) }

  val _map = Seq(
    Seq(1, 1, 1, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 0, 0, 1),
    Seq(1, 1, 1, 1)
  )

  val map = VecInit.tabulate(4, 4) { (x, y) => _map(x)(y).U }

  val raycaster = Module(new Raycaster)
  val queue = Module(new Queue(new RayHit(nTiles), 4))
  io.response <> queue.io.deq

  val trig = Module(new TrigLUT)
  val angleReg = RegInit(0.S(24.W))
  trig.io.angle := angleReg

  val vertical = near(trig.io.cos, 0.S)
  val horizontal = near(trig.io.sin, 0.S)
  val east = trig.io.cos >= 0.S
  val north = trig.io.sin >= 0.S

  val requestReg = RegInit(
    (new RayRequest)
      .Lit(
        _.start -> ((new Vec2(SInt(24.W)))
          .Lit(_.x -> 0.S(24.W), _.y -> 0.S(24.W))),
        _.angle -> 0.S(24.W)
      )
  )

  val currentRayOffsetIdx = RegInit(0.U(log2Ceil(nRays + 1).W))
  val currentRayPos = RegInit(Vec2(0.S(24.W), 0.S(24.W)))
  val currentRayDist = RegInit(0.S(24.W))
  val currentRayHorizontal = RegInit(false.B)
  val currentRayTile = RegInit(0.U(log2Ceil(nTiles).W))

  object S extends ChiselEnum {
    val idle, initRay, step, check, emit = Value
  }
  val state = RegInit(S.idle)

  io.request.ready := false.B
  raycaster.io.out.ready := false.B
  raycaster.io.stop := false.B

  queue.io.enq.valid := false.B
  queue.io.enq.bits := DontCare
  raycaster.io.in.valid := false.B
  raycaster.io.in.bits := DontCare

  switch(state) {
    is(S.idle) {
      io.request.ready := true.B

      when(io.request.fire) {
        requestReg := io.request.bits
        currentRayOffsetIdx := 0.U
        state := S.initRay
      }
    }
    is(S.initRay) {
      raycaster.io.in.bits.start := requestReg.start
      val angle = requestReg.angle + offsetsVec(currentRayOffsetIdx)
      raycaster.io.in.bits.angle := angle
      angleReg := angle
      raycaster.io.in.valid := true.B
      when(raycaster.io.in.ready) {
        currentRayOffsetIdx := currentRayOffsetIdx + 1.U
        state := S.step
      }
    }
    is(S.step) {
      when(raycaster.io.out.valid) {
        currentRayPos := raycaster.io.out.bits.pos
        currentRayHorizontal := raycaster.io.out.bits.isHorizontal
        currentRayDist := raycaster.io.out.bits.dist
        state := S.check
      }
    }
    is(S.check) {
      raycaster.io.out.ready := true.B
      val hitIdx = {
        val idxFP = Mux(
          currentRayHorizontal,
          Mux(
            north,
            Vec2(currentRayPos.x.fpFloor, currentRayPos.y),
            Vec2(currentRayPos.x.fpFloor, currentRayPos.y - toFP(1.0))
          ),
          Mux(
            east,
            Vec2(currentRayPos.x, currentRayPos.y.fpFloor),
            Vec2(currentRayPos.x - toFP(1.0), currentRayPos.y.fpFloor)
          )
        )
        Vec2(idxFP.x(23, 12), idxFP.y(23, 12))
      }
      val tileHit = map(hitIdx.y)(hitIdx.x)
      when(tileHit =/= 0.U) {
        raycaster.io.stop := true.B
        currentRayTile := tileHit
        state := S.emit
      }.otherwise {
        state := S.step
      }
    }
    is(S.emit) {
      queue.io.enq.valid := true.B
      queue.io.enq.bits.dist := currentRayDist
      queue.io.enq.bits.tile := currentRayTile
      when(queue.io.enq.ready) {
        when(currentRayOffsetIdx === nRays.U) {
          state := S.idle
        }.otherwise {
          state := S.initRay
        }
      }
    }
  }
}

class RaycastDriverSpec extends AnyFunSpec with ChiselSim with Matchers {

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

  describe("RaycastDriver") {
    it("pass general test cases") {
      for (t <- testCases) {
        simulate(new RaycastDriver(fov = t.fov, nRays = t.nRays)) { dut =>
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
