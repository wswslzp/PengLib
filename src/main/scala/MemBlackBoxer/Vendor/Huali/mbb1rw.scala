package MemBlackBoxer.Vendor.Huali

import spinal.core._
import MemBlackBoxer.MemManager._

class mbb1rw(wrap: Ram1rw) extends SinglePortBB(wrap.mc) {
//  this.setDefinitionName()
  val io = new Bundle {
    val CLK = in Bool()
    val ADR = in UInt(wrap.mc.addrWidth bit)
    val D = in Bits(wrap.mc.dataWidth bit)
    val Q = out Bits(wrap.mc.dataWidth bit)
    val WEM = in Bits(wrap.mc.dataWidth bit)
    val WE, ME, TEST1, RME, LS = in Bool()
    val RM = in Bits(4 bit)
  }
//  println("In Huali, mbb1rw")

  val cd = ClockDomain(io.CLK)

  def connectPort(): MemBlackBox = {
    wrap.clockDomain.setSynchronousWith(cd)
    this.io.CLK   <> wrap.clockDomain.readClockWire
    this.io.ADR   <> wrap.io.ap.addr
    this.io.D     <> wrap.io.dp.din
    this.io.Q     <> wrap.io.dp.dout
    this.io.WEM   <> wrap.io.ap.mask
    this.io.WE    <> wrap.io.dp.we
    this.io.ME    <> wrap.io.ap.cs
    this.io.TEST1 := False
    this.io.RME   := False
    this.io.RM    := B"4'b0010"
    this.io.LS    := False
    this
  }

  noIoPrefix()
//  connectPort()
}
