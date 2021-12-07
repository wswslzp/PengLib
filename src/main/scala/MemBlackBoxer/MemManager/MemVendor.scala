package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._
import MemBlackBoxer.Vendor._

trait MemVendor {
  def prefixName: String
  def build(mw: Ram1rw): MemBlackBox
  def build(mw: Ram1r1w): MemBlackBox
  def build(mw: Ram2rw): MemBlackBox
  def build(mw: Rom): MemBlackBox
}

case object Huali extends MemVendor {
  import MemBlackBoxer._

  def foundry = "hu"
  def technology = "40n"
  def process = "pk4"
  def productFamily = "sadrl"
  def prefixName = foundry+technology+process+productFamily

  override def build(mw: Ram1rw): MemBlackBox = new Vendor.Huali.mbb1rw(mw).build()
  override def build(mw: Ram1r1w): MemBlackBox = new Vendor.Huali.mbb1r1w(mw).build()
  override def build(mw: Ram2rw): MemBlackBox = new Vendor.Huali.mbb2rw(mw).build()
  override def build(mw: Rom): MemBlackBox = new Vendor.Huali.mbbrom(mw).build()
}