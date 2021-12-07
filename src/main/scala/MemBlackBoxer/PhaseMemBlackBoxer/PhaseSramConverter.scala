package MemBlackBoxer.PhaseMemBlackBoxer

import spinal.core._
import spinal.lib._
import internals._
import Utils._
import MemBlackBoxer._
import MemManager._

//todo add support for multiple vendor support
//  consider using SpinalTag to annotate the memory.
class PhaseSramConverter(vendor: MemVendor = Huali) extends PhaseMemBlackBoxingWithPolicy(vendor.policy) {
  override def doBlackboxing(memTopology: MemTopology): String = {
    val mem = memTopology.mem
    def wrapBool(that: Expression): Bool = that match {
      case that: Bool => that
      case that       =>
        val ret = Bool()
        ret.assignFrom(that)
        ret
    }
    def wrapConsumers(oldSource: Expression, newSource: Expression): Unit ={
      memTopology.consumers.get(oldSource) match {
        case None        =>
        case Some(array) => array.foreach(ec => {
          ec.remapExpressions{
            case e if e == oldSource => newSource
            case e                   => e
          }
        })
      }
    }

    def removeMem(): Unit ={
      mem.removeStatement()
      mem.foreachStatements(s => s.removeStatement())
    }

    val memConfig = MemConfig(
      dw = mem.width, aw = mem.addressWidth, vendor = vendor
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
          wrapConsumers(port, ram.io.dp.dout)

          removeMem()
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
          wrapConsumers(portA, ram.io.dpa.dout)

          ram.io.clkb := portB.clockDomain.readClockWire
          ram.io.apb.addr.assignFrom(portB.address)
          ram.io.apb.cs.assignFrom(wrapBool(portB.chipSelect) && portB.clockDomain.isClockEnableActive)
          ram.io.dpb.we.assignFrom(portB.writeEnable)
          ram.io.dpb.din.assignFrom(portB.data)
          wrapConsumers(portB, ram.io.dpb.dout)

          removeMem()
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
          ram.io.apa.cs.assignFrom(rd.clockDomain.isClockEnableActive)
          wrapConsumers(rd, ram.io.dp.dout)

          ram.io.clkb := wr.clockDomain.readClockWire
          ram.io.apb.addr.assignFrom(wr.address)
          ram.io.apb.cs.assignFrom(wr.clockDomain.isClockEnableActive)
          ram.io.dp.we.assignFrom(wrapBool(wr.writeEnable))
          ram.io.dp.din.assignFrom(wr.data)

          removeMem()
        }
        null
      case ErrorType => "Invalid memory type detected!"
    }

  }
}
