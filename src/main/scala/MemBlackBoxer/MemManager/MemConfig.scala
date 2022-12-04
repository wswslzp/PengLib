package MemBlackBoxer.MemManager

import spinal.core._

import scala.language.postfixOps

case class MemConfig
(
  dataWidth: Int,
  depth: Int,
  vendor: MemVendor,
  maskBitWidth: Int = 0, // bit width of write mask
  withBist: Boolean = true,
  withScan: Boolean = false,
  withPowerGate: Boolean = false,
) {
  val addrWidth = log2Up(depth)
  val bytePerWord = (dataWidth+7)/8
  val size = bytePerWord * (1 << addrWidth)
  var name = vendor.prefixName + "_wc" + depth.toString + "_dw" + dataWidth
  def bitPerMask = if(maskBitWidth == 0) 0 else math.ceil(dataWidth.toDouble / maskBitWidth).toInt
  val noMask : Boolean = maskBitWidth == 0
  val nofMask = if(maskBitWidth == 0) 0 else math.ceil(dataWidth.toDouble / bitPerMask).toInt

  def maskName: String = bitPerMask match {
    case 16 => "wordMask" // todo may check this mask is half word or word mask (currently copy from jijingg)
    case  8 => "ByteMask"
    case  1 => "bitMask"
    case  0 => "noMask"
    case  _ => SpinalError("Undefined write mask type")
  }

  def genMask: Bits = Bits(maskBitWidth bit)
}
