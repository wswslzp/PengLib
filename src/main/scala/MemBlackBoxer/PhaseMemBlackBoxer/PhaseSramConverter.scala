package MemBlackBoxer.PhaseMemBlackBoxer

import spinal.core._
import spinal.lib._
import internals._
import Utils._
import MemBlackBoxer._
import MemManager._

object dontBB extends SpinalTag

object blackboxAllWithoutUnusedTag extends MemBlackboxingPolicy {
  override def translationInterest(topology: MemTopology): Boolean = {
    if (topology.mem.getTags().contains(dontBB)) false
    else true
  }

  override def onUnblackboxable(topology: MemTopology, who: Any, message: String): Unit = generateUnblackboxableError(topology, who, message)
}

object blackboxAllWithVendorTag extends MemBlackboxingPolicy {
  override def translationInterest(topology: MemTopology): Boolean = {
    var ret = false
    topology.mem.getTags().foreach({
      case _: MemVendor => ret = true
      case _=>
    })
    ret
  }

  override def onUnblackboxable(topology: MemTopology, who: Any, message: String): Unit = generateUnblackboxableError(topology, who, message)
}

class PhaseSramConverter(globalMemVendor: MemVendor = Huali, policy: MemBlackboxingPolicy = blackboxAll) extends PhaseMemBlackBoxingWithPolicy(policy) {
  override def doBlackboxing(memTopology: MemTopology): String = {
    val mem = memTopology.mem
    def getVendor: MemVendor = {
      var ret: MemVendor = globalMemVendor
      mem.getTags().foreach({
        case v: MemVendor => ret = v
        case _=>
      })
      ret
    }
    def wrapBool(that: Expression): Bool = that match {
      case that: Bool => that
      case that       =>
        val ret = Bool()
        ret.assignFrom(that)
        ret
    }

    val memConfig = MemConfig(
      dataWidth = mem.width, addrWidth = mem.addressWidth, vendor = getVendor
    )

    getMemType(memTopology) match {
      case ROM => "Can't create ROM with initial content"
      case SinglePort =>
        mem.component.rework {
          val port = memTopology.readWriteSync.head
          val ram = Ram1rw(memConfig)

          ram.io.ap.addr.assignFrom(port.address)
          ram.io.ap.cs.assignFrom(wrapBool(port.chipSelect) && port.clockDomain.isClockEnableActive)
          ram.io.dp.we.assignFrom(port.writeEnable)
          ram.io.dp.din.assignFrom(port.data)
          // todo mask write
          wrapConsumers(memTopology, port, ram.io.dp.dout)

          removeMem(mem)
        }
        null
      case DualPort =>
        mem.component.rework{
          val portA = memTopology.readWriteSync(0)
          val portB = memTopology.readWriteSync(1)
          val ram = Ram2rw(memConfig)

          ram.io.clka := portA.clockDomain.readClockWire
          ram.io.apa.addr.assignFrom(portA.address)
          ram.io.apa.cs.assignFrom(wrapBool(portA.chipSelect) && portA.clockDomain.isClockEnableActive)
          ram.io.dpa.we.assignFrom(portA.writeEnable)
          ram.io.dpa.din.assignFrom(portA.data)
          wrapConsumers(memTopology, portA, ram.io.dpa.dout)

          ram.io.clkb := portB.clockDomain.readClockWire
          ram.io.apb.addr.assignFrom(portB.address)
          ram.io.apb.cs.assignFrom(wrapBool(portB.chipSelect) && portB.clockDomain.isClockEnableActive)
          ram.io.dpb.we.assignFrom(portB.writeEnable)
          ram.io.dpb.din.assignFrom(portB.data)
          wrapConsumers(memTopology, portB, ram.io.dpb.dout)

          removeMem(mem)
        }
        null
      case TwoPort =>
        mem.component.rework {
          val rd = memTopology.readsSync.head
          val wr = memTopology.writes.head
          //        val tmpCfg = MemConfig(32, 32, Huali)
          val ram = Ram1r1w(memConfig)

          ram.io.clka := rd.clockDomain.readClockWire
          ram.io.apa.addr.assignFrom(rd.address)
          ram.io.apa.cs.assignFrom(rd.clockDomain.isClockEnableActive && wrapBool(wr.writeEnable))
          wrapConsumers(memTopology, rd, ram.io.dp.dout)

          ram.io.clkb := wr.clockDomain.readClockWire
          ram.io.apb.addr.assignFrom(wr.address)
          ram.io.apb.cs.assignFrom(wr.clockDomain.isClockEnableActive && wrapBool(rd.readEnable))
          ram.io.dp.we.assignFrom(wrapBool(wr.writeEnable))
          ram.io.dp.din.assignFrom(wr.data)

          removeMem(mem)
        }
        null
      case ErrorType => "Invalid memory type detected!"
    }

  }
}

class PhaseMemTopoPrinter() extends PhaseMemBlackBoxingWithPolicy(blackboxAll) {
  override def doBlackboxing(memTopology: MemTopology) = {
//    SpinalInfo(s"${this.getClass} is not able to blackbox ${topology.mem}\n  write ports : ${topology.writes.size} \n  readAsync ports : ${topology.readsAsync.size} \n  readSync ports : ${topology.readsSync.size} \n  readWrite ports : ${topology.readWriteSync.size}\n  -> $message")
    SpinalInfo(
      s"""
         |=======================================================
         |The ${memTopology.mem} topology is
         |write ports         : ${memTopology.writes.size}
         |readAsync ports     : ${memTopology.readsAsync.size}
         |readSync ports      : ${memTopology.readsSync.size}
         |readWriteSync ports : ${memTopology.readWriteSync.size}
         |=======================================================""".stripMargin)
    null
  }
}
