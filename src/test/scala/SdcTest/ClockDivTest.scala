package SdcTest

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.language._

object ClockDivTest {
  import  Util._

  def main(args: Array[String]): Unit = {
//    PrintRTL("rtl")(ClockDivider(9))
    SimConfig.withIVerilog.withWave.allOptimisation.compile(ClockDivider(4, 0.25)).doSim("test"){ dut=>
      import dut._
      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(100)
      simSuccess()
    }
  }
}
