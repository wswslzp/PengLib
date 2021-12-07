package MemBlackBoxer.Vendor.Huali

import spinal.core._
import spinal.lib._
import MemBlackBoxer._
import MemBlackBoxer.MemManager._
import Vendor._

class mbb2rw(wrap: Ram2rw) extends DualPortBB(wrap.mc) {
  //  this.setDefinitionName()
  val io = new Bundle {
    val CLKA, CLKB = in Bool()
    val ADRA, ADRB = in UInt(wrap.mc.aw bit)
    val DA, DB = in Bits(wrap.mc.dw bit)
    val QA, QB = out Bits(wrap.mc.dw bit)
    val WEMA, WEMB = if(wrap.mc.needBwe) in Bits(wrap.mc.dw bit) else null
    val WEA, WEB, MEA, MEB, TEST1A, TEST1B, RMEA, RMEB, LSA, LSB = in Bool()
    val RMA, RMB = in Bits(4 bit)
  }

  val cda = ClockDomain(io.CLKA)
  val cdb = ClockDomain(io.CLKB)

  def build(): MemBlackBox = {
    wrap.cda.setSynchronousWith(cda)
    wrap.cdb.setSynchronousWith(cdb)
    this.io.CLKA   <> wrap.cda.readClockWire
    this.io.CLKB   <> wrap.cdb.readClockWire
    this.io.ADRA   <> wrap.io.apa.addr
    this.io.ADRB   <> wrap.io.apb.addr
    this.io.DA     <> wrap.io.dpa.din
    this.io.QA     <> wrap.io.dpa.dout
    this.io.DB     <> wrap.io.dpb.din
    this.io.QB     <> wrap.io.dpb.dout
    if(wrap.mc.needBwe){
      val bwea = if (wrap.mc.needBwe) wrap.io.apa.bwe else B(wrap.mc.dw bit, default -> true)
      val bweb = if (wrap.mc.needBwe) wrap.io.apb.bwe else B(wrap.mc.dw bit, default -> true)
      this.io.WEMA   <> bwea
      this.io.WEMB   <> bweb
    }
    this.io.WEA    <> wrap.io.dpa.we
    this.io.WEB    <> wrap.io.dpb.we
    this.io.MEA    <> wrap.io.apa.cs
    this.io.MEB    <> wrap.io.apb.cs
    this.io.TEST1A := True
    this.io.RMEA   := True
    this.io.RMA    := B"4'b0010"
    this.io.TEST1B := True
    this.io.RMEB   := True
    this.io.RMB    := B"4'b0010"
    this.io.LSB    := False
    this.io.LSA    := False
    this
  }

  noIoPrefix()

}