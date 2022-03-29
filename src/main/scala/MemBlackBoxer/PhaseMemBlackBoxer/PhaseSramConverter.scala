package MemBlackBoxer.PhaseMemBlackBoxer

import spinal.core._
import spinal.lib._
import internals._
import Utils._
import MemBlackBoxer._
import MemManager._
import Vendor._

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

class PhaseSramConverter(globalMemVendor: MemVendor = Vendor.Huali, policy: MemBlackboxingPolicy = blackboxAll) extends PhaseMemBlackBoxingWithPolicy(policy) {
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
      dataWidth = mem.width,
      depth = mem.wordCount,
      vendor = getVendor
    )

    getMemType(memTopology) match {
      case ROM => "Can't create ROM with initial content"
      case SinglePort =>
        mem.component.rework {
          val port = memTopology.readWriteSync.head
          val ram = Ram1rw(memConfig)

          ram.io.ap.address.assignFrom(port.address)
          ram.io.ap.memoryEnable.assignFrom(wrapBool(port.chipSelect) && port.clockDomain.isClockEnableActive)
          ram.io.dp.writeEnable.assignFrom(port.writeEnable)
          ram.io.dp.writeData.assignFrom(port.data)
          if (port.mask != null) {
            ram.io.ap.mask.assignFrom(port.mask)
          } else {
            ram.io.ap.mask.setAllTo(true)
          }
          wrapConsumers(memTopology, port, ram.io.dp.readData)

          removeMem(mem)
        }
        null
      case DualPort =>
        mem.component.rework{
          val portA = memTopology.readWriteSync(0)
          val portB = memTopology.readWriteSync(1)
          val ram = Ram2rw(memConfig)

          ram.io.clka := portA.clockDomain.readClockWire
          ram.io.apa.address.assignFrom(portA.address)
          ram.io.apa.memoryEnable.assignFrom(wrapBool(portA.chipSelect) && portA.clockDomain.isClockEnableActive)
          ram.io.dpa.writeEnable.assignFrom(portA.writeEnable)
          ram.io.dpa.writeData.assignFrom(portA.data)
          if (portA.mask != null) {
            ram.io.apa.mask.assignFrom(portA.mask)
          } else {
            ram.io.apa.mask.setAllTo(true)
          }
          wrapConsumers(memTopology, portA, ram.io.dpa.readData)

          ram.io.clkb := portB.clockDomain.readClockWire
          ram.io.apb.address.assignFrom(portB.address)
          ram.io.apb.memoryEnable.assignFrom(wrapBool(portB.chipSelect) && portB.clockDomain.isClockEnableActive)
          ram.io.dpb.writeEnable.assignFrom(portB.writeEnable)
          ram.io.dpb.writeData.assignFrom(portB.data)
          if (portB.mask != null) {
            ram.io.apb.mask.assignFrom(portB.mask)
          } else {
            ram.io.apb.mask.setAllTo(true)
          }
          wrapConsumers(memTopology, portB, ram.io.dpb.readData)

          removeMem(mem)
        }
        null
      case TwoPort =>
        mem.component.rework {
          val rd = memTopology.readsSync.head
          val wr = memTopology.writes.head
          val ram = Ram1r1w(memConfig)

          ram.io.clka := rd.clockDomain.readClockWire
          ram.io.apa.address.assignFrom(rd.address)
          ram.io.apa.memoryEnable.assignFrom(rd.clockDomain.isClockEnableActive && wrapBool(rd.readEnable))
          ram.io.apa.mask.setAllTo(true)
          wrapConsumers(memTopology, rd, ram.io.dp.readData)

          ram.io.clkb := wr.clockDomain.readClockWire
          ram.io.apb.address.assignFrom(wr.address)
          ram.io.apb.memoryEnable.assignFrom(wr.clockDomain.isClockEnableActive && wrapBool(wr.writeEnable))
          ram.io.dp.writeEnable.assignFrom(wrapBool(wr.writeEnable))
          ram.io.dp.writeData.assignFrom(wr.data)
          if (wr.mask != null) {
            ram.io.apb.mask.assignFrom(wr.mask)
          } else {
            ram.io.apb.mask setAllTo true
          }

          removeMem(mem)
        }
        null
      case ErrorType => "Invalid memory type detected!"
    }

  }
}

class PhaseMemTopoPrinter() extends PhaseMemBlackBoxingWithPolicy(blackboxAll) {
  override def doBlackboxing(memTopology: MemTopology) = {
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
