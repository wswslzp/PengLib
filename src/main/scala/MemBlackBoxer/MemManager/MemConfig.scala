package MemBlackBoxer.MemManager

import spinal.core._

import scala.language.postfixOps

case class MemConfig
(
  dataWidth: Int,
  depth: Int,
  vendor: MemVendor,
  withBist: Boolean = true,
  withScan: Boolean = false,
  withPowerGate: Boolean = false,
) {
  val addrWidth = log2Up(depth)
  val bytePerWord = (dataWidth+7)/8
  val size = bytePerWord * (1 << addrWidth)
  var name = vendor.prefixName + "_wc" + depth.toString + "_dw" + dataWidth
  def genMask: Bits = Bits(dataWidth bit)
}
