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

  case class P4() extends Module {
    val a = in UInt(8 bit)
    val b = out UInt(8 bit)

    val aclk = ClockDomain.external("a")
    val a1 = UInt(8 bit)
    aclk {
      a1 := Delay(a, 8)
    }

    val bclk = ClockDomain.external("b")
    val a2 = UInt(8 bit)
    bclk {
      a2 := Delay(BufferCC(a1), 4)
    }

    b := BufferCC(a2)
  }

  case class P5() extends Module {
    val a = in UInt(8 bit)
    val b = out UInt(8 bit)
    val en = in Bool()

    val a1 = Delay(a, 1, init = a.getZero)
    val d4 = new ClockEnableArea(en){
      b := Delay(a1, 2, init = a1.getZero)
    }
  }

  def main(args: Array[String]): Unit = {
//    SimConfig
//      .withWave
//      .withIVerilog
//      .allOptimisation
//      .workspacePath("tb")
//      .compile(PPP())
//      .doSim("test"){dut=>
//        import dut._
//        clockDomain.forkStimulus(2)
//        a #= 0
//        clockDomain.waitSampling()
//
//        for(i <- 0 to 9){
//          a #= i + 3
//          clockDomain.waitSampling()
//        }
//        clockDomain.waitSampling(100)
//        simSuccess()
//      }

    PrintRTL("rtl")(P5())
  }

}
