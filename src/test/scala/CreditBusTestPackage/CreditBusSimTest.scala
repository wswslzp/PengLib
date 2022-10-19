package CreditBusTestPackage

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import spinal.lib.sim._

import scala.collection.mutable

class CreditBusSimTest extends AnyFunSuite {

  import scala.util.Random

  test("Stream to Credit pipe test") {

    val expectData = List.fill(Random.nextInt(100))(BigInt(32, Random))
    val actualData = mutable.Queue[BigInt]()

    SimConfig.withWave.withIVerilog.compile(CreditStreamPipe()).doSim("sanity test"){dut=>
      import dut._

      val upDriver = StreamDriver.queue(up, clockDomain)._2
      val readyDriver = StreamReadyRandomizer(down, clockDomain)
      val downMonitor = StreamMonitor(down, clockDomain){payload=>
        actualData.enqueue(payload.toBigInt)
      }

      clockDomain.forkStimulus(2)
      clockDomain.waitSampling()

      for(i <- expectData.indices){
        upDriver += {pd=>
          pd #= expectData(i)
        }
      }

      clockDomain.waitSampling(100000)

      assert(actualData.toList == expectData)
    }
  }

  test("Stream <-> Credit Pipe stress test") {

    val expectData = List.fill(Random.nextInt(100))(BigInt(32, Random))
    val actualData = mutable.Queue[BigInt]()

    SimConfig.withWave.withIVerilog.compile(CreditStreamPipe()).doSim("stress test"){dut=>
      import dut._

      val upDriver = StreamDriver.queue(up, clockDomain)._2
      val readyDriver = StreamReadyRandomizer(down, clockDomain)
      val downMonitor = StreamMonitor(down, clockDomain){payload=>
        actualData.enqueue(payload.toBigInt)
      }

      clockDomain.forkStimulus(2)
      clockDomain.waitSampling()

      for(i <- expectData.indices){
        upDriver += {pd=>
          pd #= expectData(i)
        }
      }

      clockDomain.waitSampling(100000)

      assert(actualData.toList == expectData)
    }
  }

}
