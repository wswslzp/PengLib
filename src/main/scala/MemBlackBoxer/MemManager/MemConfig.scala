package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

case class MemConfig
(
  dataWidth: Int,
  addrWidth: Int,
  vendor: MemVendor,
  withBist: Boolean = true,
  withScan: Boolean = false,
  withPowerGate: Boolean = false,
  needBwe: Boolean = false
) {
  val bytePerWord = (dataWidth+7)/8
  val size = bytePerWord * (1 << addrWidth)
  var name = vendor.prefixName + "_" + "aw" + addrWidth.toString + "_dw" + dataWidth
  def genBwe: Bits = Bits(dataWidth bit).genIf(needBwe)
}
