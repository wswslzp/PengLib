import spinal.core._
import scala.language.{postfixOps, _}
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
//    val en = in Bool()

    val a1 = Delay(a, 0, init = a.getZero)
    b := a1
//    val d4 = new ClockEnableArea(en){
//      b := Delay(a1, 1, init = a1.getZero)
//    }
  }

  case class P6() extends Module {
    val a, b, c = in UInt(8 bit)
    val d, e = out UInt(12 bit)
    (d, e) := (a, b, c).asBits
  }

  case class P7() extends Module {
    val a,b = Vec.fill(8)(in UInt(8 bit))
    val mac = (a, b).zipped.map(_ * _).reduceBalancedTree(_ + _) asOutput() setAsReg()
  }

  case class P8() extends Module {
    val data = in(Bits(64 bits))
    val outData = out(UInt(64 bits))
    report(L"out data is $outData")

    data.subdivideIn(4 slices)
    outData := data.subdivideIn(8 bits).shuffle { i =>
      val total = data.getWidth / 8
      total - 1 -i
    }.asBits.asUInt
    data.addTag(new CommentTag("there is a data."))
    data.addAttribute(new AttributeFlag("therer i a data", COMMENT_ATTRIBUTE))
    addComment(f"P8 comment hrere")

  }

  abstract class AP1 extends Module {
    val data = in Bits(9 bit)
    val io = new Bundle {
    }
  }
  class P9 extends AP1 {
    override val io = new Bundle {
      val x = out Bits(8 bit)
    }
  }

  object DelayWithInit1 {
    def apply[T <: Data](that: T, cycleCount: Int, when: Bool = null)(onEachReg: T => Unit = null): T = {
//      Delay[T](that, cycleCount, onEachReg = onEachReg)
      require(cycleCount >= 0,"Negative cycleCount is not allowed in Delay")
      var ptr = that
      for(i <- 0 until cycleCount) {
        if (when == null)
//          ptr = RegNext(ptr, init)
          ptr = RegNext(ptr)
        else
//          ptr = RegNextWhen(ptr, when, init)
          ptr = RegNextWhen(ptr, when)

        ptr.unsetName().setCompositeName(that, "delay_" + (i + 1), true)
        if(onEachReg != null) {
          onEachReg(ptr)
        }
      }
      ptr
    }
  }

  object Delay1 {
    def apply[T <: Data](that: T, cycleCount: Int, when : Bool = null, init: T = null, onEachReg : T => Unit = null): T = {
//    def apply[T <: Data](that: T, cycleCount: Int, when : Bool = null, onEachReg : T => Unit = null, init: T = null): T = {
      DelayWithInit1(that, cycleCount, when){dt=>
        onEachReg(dt)
        if (init != null) dt.init(init)
      }
    }
  }

  object Delay2 {
//    def apply[T <: Data](that: T, cycleCount: Int,when : Bool = null,init : T = null.asInstanceOf[T],onEachReg : T => Unit = null.asInstanceOf[T => Unit]): T = {
    def apply[T <: Data](that: T, cycleCount: Int, when : Bool = null, init: T = null, onEachReg : T => Unit = null): T = {
      require(cycleCount >= 0,"Negative cycleCount is not allowed in Delay")
      var ptr = that
      for(i <- 0 until cycleCount) {
        if (when == null)
          ptr = RegNext(ptr, init)
        else
          ptr = RegNextWhen(ptr, when, init)

        ptr.unsetName().setCompositeName(that, "delay_" + (i + 1), true)
        if(onEachReg != null) {
          onEachReg(ptr)
        }
      }
      ptr
    }
  }

  case class P10() extends Module {
    val a = in Bool()
    val clear = in Bool()
    val b = out Bool()
//    b := DelayWithInit(a, 4){d=>
//      println(d.getBitsWidth)
//    }
    val c = Delay2(
      a, 4,
      onEachReg = (d: Bool)=> {
        println(d.getBitsWidth)
      }
    )
    b := c
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

    PrintRTL("rtl1")(P10())
//    SpinalVerilog(P5())
  }

}
