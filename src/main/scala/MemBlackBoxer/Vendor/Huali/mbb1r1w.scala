package MemBlackBoxer.Vendor.Huali

import spinal.core._
import spinal.lib._
import MemBlackBoxer._
import Vendor._
import MemBlackBoxer.MemManager._

class mbb1r1w(wrap: Ram1r1w) extends TwoPortBB(wrap.mc) {
  //  this.setDefinitionName()
  val io = new Bundle {
    val CLKA, CLKB = in Bool()
    val ADRA, ADRB = in UInt(wrap.mc.addrWidth bit)
    val DA = in Bits(wrap.mc.dataWidth bit)
    val QB = out Bits(wrap.mc.dataWidth bit)
    val WEMA, WEMB = if(wrap.mc.needBwe) in Bits(wrap.mc.dataWidth bit) else null
    val WEA, MEA, MEB, TEST1A, TEST1B, RMEA, RMEB, LS = in Bool()
    val RMA, RMB = in Bits(4 bit)
  }

  val cda = ClockDomain(io.CLKA)
  val cdb = ClockDomain(io.CLKB)

  def connectPort(): MemBlackBox = {
    wrap.cda.setSynchronousWith(cda)
    wrap.cdb.setSynchronousWith(cdb)
    this.io.CLKA   <> wrap.cda.readClockWire
    this.io.CLKB   <> wrap.cdb.readClockWire
    this.io.ADRA   <> wrap.io.apa.addr
    this.io.ADRB   <> wrap.io.apb.addr
    this.io.DA     <> wrap.io.dp.din
    this.io.QB     <> wrap.io.dp.dout
    if(wrap.mc.needBwe){
      val bwea = if (wrap.mc.needBwe) wrap.io.apa.bwe else B(wrap.mc.dataWidth bit, default -> true)
      val bweb = if (wrap.mc.needBwe) wrap.io.apb.bwe else B(wrap.mc.dataWidth bit, default -> true)
      this.io.WEMA   <> bwea
      this.io.WEMB   <> bweb
    }
    this.io.WEA    <> wrap.io.dp.we
    this.io.MEA    <> wrap.io.apa.cs
    this.io.MEB    <> wrap.io.apb.cs
    this.io.TEST1A := False
    this.io.RMEA   := False
    this.io.RMA    := B"4'b0010"
    this.io.TEST1B := False
    this.io.RMEB   := False
    this.io.RMB    := B"4'b0010"
    this.io.LS    := False
    this
  }

  noIoPrefix()
//  connectPort()
}
