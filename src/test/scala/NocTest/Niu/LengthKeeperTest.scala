package NocTest.Niu
import org.scalatest.funsuite._
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

import scala.util.Random
import scala.collection.mutable
import NoC.Niu._
import NocTest.StreamDriver1


class LengthKeeperTest extends AnyFunSuite {
  val mLen = 32
  val rtl = SimConfig.withIVerilog.withWave.compile(LengthKeeper(mLen))

  case class Packet(maxLen: Int) {
    private val packetLen = Random.nextInt(maxLen)
    private val data = Random.nextInt(maxLen)
    val packet = List.fill(packetLen)(data)

    def issueOnce(d: UInt): Unit = {
      d #= data
    }
    def issue(): List[UInt => Unit] = List.fill(packetLen)(issueOnce)
  }

  test("length keeper tester") {
    rtl.doSim(2) { dut =>
      import dut._
      clockDomain.forkStimulus(2)
//      val (driver, cmd) = StreamDriver1.queue(io.input, clockDomain)

      val readyRandomizer = StreamReadyRandomizer(io.output, clockDomain)
      val iMonitor = StreamMonitor(io.input, clockDomain){dat=>

      }
      val oMonitor = StreamMonitor(io.output, clockDomain){dat=>

      }
      io.flitEnd #= false
      io.burstEnd #= false
      io.input.valid #= false
      io.input.payload.randomize()

      fork{
        clockDomain.waitSampling(10000)
        simFailure()
      }
      clockDomain.waitSampling()

//      driver.reset()
//      for(i <- 0 until 1){
//        cmd ++= Packet(mLen).issue()
//      }
//
//      clockDomain.waitSamplingWhere(cmd.isEmpty)
//      simSuccess()
      io.flitEnd #= true
      clockDomain.waitSampling()
      io.flitEnd #= false

      forkJoin(
        () => {
          for(i <- 0 until 100){
            val len = Random.nextInt(mLen)
            for(j <- 0 until len){
              io.input.valid #= true
              io.input.payload #= len
              io.burstEnd #= j == len-1
              io.testID #= j
              clockDomain.waitSamplingWhere(io.input.ready.toBoolean)
              io.input.valid #= false
              clockDomain.waitSampling(Random.nextInt(10))
            }
            io.input.valid #= false
            fork{
              clockDomain.waitSampling(Random.nextInt(128))
              io.flitEnd #= true
              clockDomain.waitSampling()
              io.flitEnd #= false
            }
            clockDomain.waitSampling(Random.nextInt(10))
          }
          simSuccess()
        }
      )

    }
  }
}
