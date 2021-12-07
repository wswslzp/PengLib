package MemBlackBoxer.Vendor.Huali

import spinal.core._
import spinal.lib._
import MemBlackBoxer.MemManager._
import MemBlackBoxer._
import Vendor._

class mbb1rw(wrap: Ram1rw) extends SinglePortBB(wrap.mc) {
//  this.setDefinitionName()
  val io = new Bundle {
    val CLK = in Bool()
    val ADR = in UInt(wrap.mc.aw bit)
    val D = in Bits(wrap.mc.dw bit)
    val Q = out Bits(wrap.mc.dw bit)
    val WEM = if(wrap.mc.needBwe) in Bits(wrap.mc.dw bit) else null
    val WE, ME, TEST1, RME, LS = in Bool()
    val RM = in Bits(4 bit)
  }
//  println("In Huali, mbb1rw")

  val cd = ClockDomain(io.CLK)

  def build(): MemBlackBox = {
    wrap.clockDomain.setSynchronousWith(cd)
    this.io.CLK   <> wrap.clockDomain.readClockWire
    this.io.ADR   <> wrap.io.ap.addr
    this.io.D     <> wrap.io.dp.din
    this.io.Q     <> wrap.io.dp.dout
    if(wrap.mc.needBwe){
      val bwe = if (wrap.mc.needBwe) wrap.io.ap.bwe else B(wrap.mc.dw bit, default -> true)
      this.io.WEM   <> bwe
    }
    this.io.WE    <> wrap.io.dp.we
    this.io.ME    <> wrap.io.ap.cs
    this.io.TEST1 := True
    this.io.RME   := True
    this.io.RM    := B"4'b0010"
    this.io.LS    := False
    this
  }

  noIoPrefix()
}
