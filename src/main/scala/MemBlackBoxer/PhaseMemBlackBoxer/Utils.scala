package MemBlackBoxer.PhaseMemBlackBoxer

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import internals._

object Utils {

  trait MemType
  object TwoPort extends MemType // Two port SRAM
  object SinglePort extends MemType // Single port SRAM
  object DualPort extends MemType // Dual port SRAM
  object ROM extends MemType
  object ErrorType extends MemType

  def getMemType(topology: MemTopology): MemType = {
    val mem = topology.mem
    if(mem.initialContent != null) {
      SpinalInfo("Got an rom")
      ROM
    } else if( // TWO Port
      topology.writes.size == 1 && topology.readsSync.size == 1 && topology.readWriteSync.isEmpty && topology.writeReadSameAddressSync.isEmpty
    ) {
      SpinalInfo("Got an two port ram")
      TwoPort
    } else if ( // Single port
      topology.portCount == 1 && topology.readWriteSync.size == 1
    ) {
      SpinalInfo("Got an single port ram")
      SinglePort
    } else if ( // Dual port
      topology.portCount == 2 && topology.readWriteSync.size == 2
    ) {
      SpinalInfo("Got an dual port ram")
      DualPort
    } else {
      SpinalInfo("ERROR ram!")
      ErrorType
    }
  }

}
