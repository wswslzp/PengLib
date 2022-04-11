package SdcTest

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.language.postfixOps

case class SC() extends Module {
  val cnt0 = CounterFreeRun(8)
  out(cnt0.value)

  val c1 = ClockDomain.current.newClockDomainSlowedBy(4)
  val s1 = new ClockingArea(c1){
    val cnt1 = CounterFreeRun(8)
    out(cnt1.value)
  }
}

object SlowClock {
  def main(args: Array[String]): Unit = {
//    Util.PrintRTL("rtl")(SC())

    SimConfig.withIVerilog.withWave.allOptimisation.compile(SC()).doSim("test"){dut=>
      import dut._
      clockDomain.forkStimulus(2)
      clockDomain.waitSampling(100)
      simSuccess()
    }
  }
}
