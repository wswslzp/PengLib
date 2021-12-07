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
      ROM
    } else if( // TWO Port
      topology.writes.size == 1 && topology.readsSync.size == 1 && topology.readWriteSync.isEmpty && topology.writeReadSameAddressSync.isEmpty
    ) {
      TwoPort
    } else if ( // Single port
      topology.portCount == 1 && topology.readWriteSync.size == 1
    ) {
      SinglePort
    } else if ( // Dual port
      topology.portCount == 2 && topology.readWriteSync.size == 2
    ) {
      DualPort
    } else {
      SpinalWarning("ERROR ram!")
      ErrorType
    }
  }

}
