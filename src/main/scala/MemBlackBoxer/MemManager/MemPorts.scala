package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

class MemPorts extends Bundle

case class AddrCtrlPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val memoryEnable = Bool()
  val mask = mc.genMask
  val address = UInt(mc.addrWidth bit)

  override def asMaster(): Unit = {
    in(memoryEnable, mask, address)
    in(memoryEnable, address)
  }
}

case class DataPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val writeData = Bits(mc.dataWidth bit)
  val readData = Bits(mc.dataWidth bit)
  val writeEnable = Bool

  override def asMaster(): Unit = {
    in(writeEnable, writeData)
    out(readData)
  }
}

case class BistPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val bistEnable = Bool
  val addrCtrlPort = AddrCtrlPorts(mc)
  val dataPort = DataPorts(mc)

  override def asMaster(): Unit = {
    in(addrCtrlPort, bistEnable)
    master(dataPort)
  }
}

case class ScanPorts(mc: MemConfig) extends MemPorts with IMasterSlave {

  override def asMaster(): Unit = ???
}
