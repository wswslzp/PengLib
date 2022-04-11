package SdcTest

import EdaAuto.PhaseSdcGenerator.insertSdcPhase
import spinal.core._
import spinal.lib._

import scala.language.postfixOps
import spinal.core.sim._
import spinal.lib.sim.StreamDriver
import Util._

case class CR() extends Module {
  val input = slave(Stream(Bits(32 bit)))
  val output = master(Stream(Bits(32 bit)))
  output <-< input
}

case class MCTop() extends Module {
  val input = slave(Stream(Bits(32 bit)))
  val output = master(Stream(Bits(32 bit)))

  val m0 = CR()

  val c1 = ClockDomain.external("ext0", frequency = FixedFrequency(300 MHz))
  val s1 = new ClockingArea(c1) {
    val m1 = CR()
    val c10 = ClockDomain.current.slowClockBy(2, "c10")
    val s10 = new ClockingArea(c10){
      val m10 = CR()
    }
    val c11 = ClockDomain.current.slowClockBy(4, "c11")
    val s11 = new ClockingArea(c11){
      val m11 = CR()
    }
  }

  val c2 = ClockDomain.current.slowClockBy(2, "c2")
  val s2 = new ClockingArea(c2) {
    val m2 = CR()
  }

  val fork5 = StreamFork(input, 5)
  m0.input << fork5(0).queue(8) // directly connect
  s1.m1.input << fork5(1).queue(8, clockDomain, c1)
  s1.s10.m10.input << fork5(2).queue(8, clockDomain, c1)
  s1.s11.m11.input << fork5(3).queue(8, clockDomain, c1)
  s2.m2.input << fork5(4).queue(8)

  val o0 = m0.output.queue(8)
  val o1 = s1.m1.output.queue(8, c1, clockDomain)
  val o2 = s1.s10.m10.output.queue(8, c1, clockDomain)
  val o3 = s1.s11.m11.output.queue(8, c1, clockDomain)
  val o4 = s2.m2.output.queue(8)

  output << StreamArbiterFactory.roundRobin.noLock.onArgs(
    o0, o1, o2, o3, o4
  )
}

object MCTop {
  def main(args: Array[String]): Unit = {
    val config = SpinalConfig(
      targetDirectory = "rtl",
      defaultClockDomainFrequency = FixedFrequency(50 MHz)
    )
    config.phasesInserters.append(insertSdcPhase)
    val report = config.generateVerilog(MCTop())
  }
}
