package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

case class MemConfig
(
  dw: Int,
  aw: Int,
  vendor: MemVendor,
  withBist: Boolean = true,
  withScan: Boolean = false,
  withPowerGate: Boolean = false,
  needBwe: Boolean = false
) {
  val bytePerWord = (dw+7)/8
  val size = bytePerWord * (1 << aw)
  var name = vendor.prefixName + "_" + size.toString + "B" // todo: name it according to dw/aw
  def genBwe: Bits = Bits(dw bit).genIf(needBwe)
}
