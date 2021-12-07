package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

import MemBlackBoxer._
import Vendor.MemBlackBox

trait MemWrap {
  val ram: MemBlackBox
  def addSimulationModel(fileName: String): Unit = ram.addRTLPath(fileName)
}

case class Ram1rw(mc: MemConfig) extends Component with MemWrap {
  val io = new Bundle {
    val ap = in(AddrCtrlPorts(mc))
    val dp = master(DataPorts(mc))
    val bist = master(BistPorts(mc))
//    val scan = master(ScanPorts(mc))
  }
  noIoPrefix()
  val ram = mc.vendor.build(this)
}

case class Ram1r1w(mc: MemConfig) extends Component with MemWrap{
  val io = new Bundle {
    val clka, clkb = in Bool()
    val apa = in(AddrCtrlPorts(mc))
    val apb = in(AddrCtrlPorts(mc))
    val dp = master(DataPorts(mc))
    val bista = master(BistPorts(mc))
    val bistb = master(BistPorts(mc))
  }
  val cda = ClockDomain.internal("cda", withReset = false)
  val cdb = ClockDomain.internal("cdb", withReset = false)
  cda.clock := io.clka
  cdb.clock := io.clkb
  noIoPrefix()
  val ram = mc.vendor.build(this)
}

case class Ram2rw(mc: MemConfig) extends Component with MemWrap {
  val io = new Bundle {
    val clka, clkb = in Bool()
    val apa = in(AddrCtrlPorts(mc))
    val dpa = master(DataPorts(mc))
    val bista = master(BistPorts(mc))
    val apb = in(AddrCtrlPorts(mc))
    val dpb = master(DataPorts(mc))
    val bistb = master(BistPorts(mc))
  }
  val cda = ClockDomain.internal("cda", withReset = false)
  val cdb = ClockDomain.internal("cdb", withReset = false)
  cda.clock := io.clka
  cdb.clock := io.clkb
  noIoPrefix()
  val ram = mc.vendor.build(this)
}

case class Rom(mc: MemConfig) extends Component with MemWrap {
  val io = new Bundle {
    val cs, wr = in Bool()
    val bwe = mc.genBwe
    val addr = in UInt(mc.aw bit)
    val rdata = in Bits(mc.dw bit)
  }
  noIoPrefix()
  val ram = mc.vendor.build(this)
}
