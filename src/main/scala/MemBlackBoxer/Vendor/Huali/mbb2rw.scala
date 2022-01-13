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
    val ADRA, ADRB = in UInt(wrap.mc.addrWidth bit)
    val DA, DB = in Bits(wrap.mc.dataWidth bit)
    val QA, QB = out Bits(wrap.mc.dataWidth bit)
    val WEMA, WEMB = in Bits(wrap.mc.dataWidth bit)
    val WEA, WEB, MEA, MEB, TEST1A, TEST1B, RMEA, RMEB, LS = in Bool()
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
    this.io.DA     <> wrap.io.dpa.din
    this.io.QA     <> wrap.io.dpa.dout
    this.io.DB     <> wrap.io.dpb.din
    this.io.QB     <> wrap.io.dpb.dout
//    if(wrap.mc.needMask){
//      val maska = if (wrap.mc.needMask) wrap.io.apa.mask else B(wrap.mc.dataWidth bit, default -> true)
//      val maskb = if (wrap.mc.needMask) wrap.io.apb.mask else B(wrap.mc.dataWidth bit, default -> true)
    this.io.WEMA   <> wrap.io.apa.mask
    this.io.WEMB   <> wrap.io.apb.mask
//    }
    this.io.WEA    <> wrap.io.dpa.we
    this.io.WEB    <> wrap.io.dpb.we
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
