package MemBlackBoxer.MemManager

import spinal.core._

/**
 * The memory vendor definition.
 * Provide a `build` method to instantiate a memory blackbox, according to the memory wrapper type.
 */
trait MemVendor extends SpinalTag {
  val policy: MemBlackboxingPolicy = blackboxAll

  def prefixName: String
  def build(mw: MemWrapper) : MemBlackBox
}

case object Huali extends MemVendor {
  import MemBlackBoxer.Vendor.Huali._

  def foundry = "hu"
  def technology = "40n"
  def process = "pk4"
  def productFamily = "sadrl"
  def prefixName = foundry+technology+process+productFamily

  override def build(mw: MemWrapper) = mw match {
    case mem: Ram1rw => new mbb1rw(mem).connectPort()
    case mem: Ram1r1w=> new mbb1r1w(mem).connectPort()
    case mem: Ram2rw => new mbb2rw(mem).connectPort()
    case mem: Rom    => new mbbrom(mem).connectPort()
  }
}