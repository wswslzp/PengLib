package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._
import MemBlackBoxer._

/**
 * The top memory wrapper, which is the main API for instantiating a memory.
 * @param mc - memory config, including the data width, address width and memory vendor etc.
 */
abstract class MemWrapper(mc: MemConfig) extends Component {
  val ram: MemBlackBox// = mc.vendor.build(this)
  def addSimulationModel(fileName: String): Unit = ram.addRTLPath(fileName)

//  def getRamBlackBox: MemBlackBox = this match {
//    case bb: Ram1rw =>
//  }
}

/**
 * Memory wrapper type for single port SRAM.
 * @param mc - memory config, including the data width, address width and memory vendor etc.
 */
case class Ram1rw(mc: MemConfig) extends MemWrapper(mc) {
  val io = new Bundle {
    val ap = in(AddrCtrlPorts(mc))
    val dp = master(DataPorts(mc))
//    val bist = master(BistPorts(mc))
//    val scan = master(ScanPorts(mc))
  }
  override val ram = mc.vendor.build(this)
  noIoPrefix()
}

/**
 * Memory wrapper type for dual port SRAM
 * @param mc - memory config, including the data width, address width and memory vendor etc.
 */
case class Ram1r1w(mc: MemConfig) extends MemWrapper(mc) {
  val io = new Bundle {
    val clka, clkb = in Bool()
    val apa = in(AddrCtrlPorts(mc))
    val apb = in(AddrCtrlPorts(mc))
    val dp = master(DataPorts(mc))
//    val bista = master(BistPorts(mc))
//    val bistb = master(BistPorts(mc))
  }
  val cda = ClockDomain.internal("cda", withReset = false)
  val cdb = ClockDomain.internal("cdb", withReset = false)
  cda.clock := io.clka
  cdb.clock := io.clkb
  noIoPrefix()
  override val ram = mc.vendor.build(this)
}

/**
 * Memory wrapper type for two port SRAM
 * @param mc - memory config, including the data width, address width and memory vendor etc.
 */
case class Ram2rw(mc: MemConfig) extends MemWrapper(mc) {
  val io = new Bundle {
    val clka, clkb = in Bool()
    val apa = in(AddrCtrlPorts(mc))
    val dpa = master(DataPorts(mc))
//    val bista = master(BistPorts(mc))
    val apb = in(AddrCtrlPorts(mc))
    val dpb = master(DataPorts(mc))
//    val bistb = master(BistPorts(mc))
  }
  override val ram = mc.vendor.build(this)
  val cda = ClockDomain.internal("cda", withReset = false)
  val cdb = ClockDomain.internal("cdb", withReset = false)
  cda.clock := io.clka
  cdb.clock := io.clkb
  noIoPrefix()
}

/**
 * Memory wrapper type for ROM.
 * @param mc - memory config, including the data width, address width and memory vendor etc.
 */
case class Rom(mc: MemConfig) extends MemWrapper(mc) {
  val io = new Bundle {
    val cs, wr = in Bool()
    val bwe = mc.genBwe
    val addr = in UInt(mc.addrWidth bit)
    val rdata = in Bits(mc.dataWidth bit)
  }
  noIoPrefix()
  override val ram = mc.vendor.build(this)
}
