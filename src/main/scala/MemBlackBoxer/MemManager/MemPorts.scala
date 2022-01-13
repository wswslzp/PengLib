package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

class MemPorts extends Bundle {

}

case class AddrCtrlPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val cs = Bool()
  val mask = mc.genMask
  val addr = UInt(mc.addrWidth bit)

  override def asMaster(): Unit = {
    in(cs, mask, addr)
    in(cs, addr)
  }
}

case class DataPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val din = Bits(mc.dataWidth bit)
  val dout = Bits(mc.dataWidth bit)
  val we = Bool

  override def asMaster(): Unit = {
    in(we, din)
    out(dout)
  }
}

case class BistPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val bist_en = Bool
  val ap = AddrCtrlPorts(mc)
  val dp = DataPorts(mc)

  override def asMaster(): Unit = {
    in(ap, bist_en)
    master(dp)
  }
}

case class ScanPorts(mc: MemConfig) extends MemPorts with IMasterSlave {

  override def asMaster(): Unit = ???
}
