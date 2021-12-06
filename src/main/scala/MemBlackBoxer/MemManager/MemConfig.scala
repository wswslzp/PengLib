package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

case class MemConfig
(
  dw: Int,
  aw: Int,
  vendor: Vendor,
  name: String = "__nameless_sram__",
  withBist: Boolean = true,
  withScan: Boolean = false,
  withPowerGate: Boolean = false,
  needBwe: Boolean = true
) {
  def genBwe: Bits = Bits(dw bit).genIf(needBwe)
}
