import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.language._

import Util._
object PPTest {

  case class PP() extends Module {
    val a = in UInt(8 bit)
    val b = out UInt(8 bit)

    val a2 = a * a
    val a4 = a2*a2
    val a8 = a4*a4

    b := a8.subdivideIn(8 slices).reduceBalancedTree(_ ^ _)
  }

  case class PPP() extends Module {
    val a = in UInt(8 bit)
    val b = out UInt(8 bit)

    val pp = PingPongComb(4)(PP())
    pp.a := a
    b := pp.b
  }

  def main(args: Array[String]): Unit = {
    SimConfig
      .withWave
      .withIVerilog
      .allOptimisation
      .workspacePath("tb")
      .compile(PPP())
      .doSim("test"){dut=>
        import dut._
        clockDomain.forkStimulus(2)
        a #= 0
        clockDomain.waitSampling()

        for(i <- 0 to 9){
          a #= i + 3
          clockDomain.waitSampling()
        }
        clockDomain.waitSampling(100)
        simSuccess()
      }
  }

}
