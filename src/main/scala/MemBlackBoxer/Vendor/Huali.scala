package MemBlackBoxer.Vendor

import MemBlackBoxer.MemManager._
import spinal.core._

import scala.language._

object Huali extends MemVendor {

  def foundry = "hu"

  def technology = "40n"

  def process = "pk4"

  def productFamily = "sadrl"

  def prefixName = foundry + technology + process + productFamily

  override def build(mw: MemWrapper) = mw match {
    case mem: Ram1rw => new mbb1rw(mem).connectPort()
    case mem: Ram1r1w => new mbb1r1w(mem).connectPort()
    case mem: Ram2rw => new mbb2rw(mem).connectPort()
    case mem: Rom => new mbbrom(mem).connectPort()
  }


  class mbb1r1w(wrap: Ram1r1w) extends TwoPortBB(wrap.mc) {
    val io = new Bundle {
      val CLKA, CLKB = in Bool()
      val ADRA, ADRB = in UInt (wrap.mc.addrWidth bit)
      val DA = in Bits (wrap.mc.dataWidth bit)
      val QB = out Bits (wrap.mc.dataWidth bit)
      val WEMA, WEMB = in Bits (wrap.mc.dataWidth bit)
      val WEA, MEA, MEB, TEST1A, TEST1B, RMEA, RMEB, LS = in Bool()
      val RMA, RMB = in Bits (4 bit)
    }

    val cda = ClockDomain(io.CLKA)
    val cdb = ClockDomain(io.CLKB)

    def connectPort(): MemBlackBox = {
      wrap.cda.setSynchronousWith(cda)
      wrap.cdb.setSynchronousWith(cdb)
      this.io.CLKA <> wrap.cda.readClockWire
      this.io.CLKB <> wrap.cdb.readClockWire
      this.io.ADRA <> wrap.io.apa.address
      this.io.ADRB <> wrap.io.apb.address
      this.io.DA <> wrap.io.dp.writeData
      this.io.QB <> wrap.io.dp.readData
      this.io.WEMA <> wrap.io.apa.mask
      this.io.WEMB <> wrap.io.apb.mask
      this.io.WEA <> wrap.io.dp.writeEnable
      this.io.MEA <> wrap.io.apa.memoryEnable
      this.io.MEB <> wrap.io.apb.memoryEnable
      this.io.TEST1A := False
      this.io.RMEA := False
      this.io.RMA := B"4'b0010"
      this.io.TEST1B := False
      this.io.RMEB := False
      this.io.RMB := B"4'b0010"
      this.io.LS := False
      this
    }

    noIoPrefix()
  }

  class mbb1rw(wrap: Ram1rw) extends SinglePortBB(wrap.mc) {
    //  this.setDefinitionName()
    val io = new Bundle {
      val CLK = in Bool()
      val ADR = in UInt (wrap.mc.addrWidth bit)
      val D = in Bits (wrap.mc.dataWidth bit)
      val Q = out Bits (wrap.mc.dataWidth bit)
      val WEM = in Bits (wrap.mc.dataWidth bit)
      val WE, ME, TEST1, RME, LS = in Bool()
      val RM = in Bits (4 bit)
    }
    //  println("In Huali, mbb1rw")

    val cd = ClockDomain(io.CLK)

    def connectPort(): MemBlackBox = {
      wrap.clockDomain.setSynchronousWith(cd)
      this.io.CLK <> wrap.clockDomain.readClockWire
      this.io.ADR <> wrap.io.ap.address
      this.io.D <> wrap.io.dp.writeData
      this.io.Q <> wrap.io.dp.readData
      this.io.WEM <> wrap.io.ap.mask
      this.io.WE <> wrap.io.dp.writeEnable
      this.io.ME <> wrap.io.ap.memoryEnable
      this.io.TEST1 := False
      this.io.RME := False
      this.io.RM := B"4'b0010"
      this.io.LS := False
      this
    }

    noIoPrefix()
    //  connectPort()
  }

  class mbb2rw(wrap: Ram2rw) extends DualPortBB(wrap.mc) {
    //  this.setDefinitionName()
    val io = new Bundle {
      val CLKA, CLKB = in Bool()
      val ADRA, ADRB = in UInt (wrap.mc.addrWidth bit)
      val DA, DB = in Bits (wrap.mc.dataWidth bit)
      val QA, QB = out Bits (wrap.mc.dataWidth bit)
      val WEMA, WEMB = in Bits (wrap.mc.dataWidth bit)
      val WEA, WEB, MEA, MEB, TEST1A, TEST1B, RMEA, RMEB, LS = in Bool()
      val RMA, RMB = in Bits (4 bit)
    }

    val cda = ClockDomain(io.CLKA)
    val cdb = ClockDomain(io.CLKB)

    def connectPort(): MemBlackBox = {
      wrap.cda.setSynchronousWith(cda)
      wrap.cdb.setSynchronousWith(cdb)
      this.io.CLKA <> wrap.cda.readClockWire
      this.io.CLKB <> wrap.cdb.readClockWire
      this.io.ADRA <> wrap.io.apa.address
      this.io.ADRB <> wrap.io.apb.address
      this.io.DA <> wrap.io.dpa.writeData
      this.io.QA <> wrap.io.dpa.readData
      this.io.DB <> wrap.io.dpb.writeData
      this.io.QB <> wrap.io.dpb.readData
      //    if(wrap.mc.needMask){
      //      val maska = if (wrap.mc.needMask) wrap.io.apa.mask else B(wrap.mc.dataWidth bit, default -> true)
      //      val maskb = if (wrap.mc.needMask) wrap.io.apb.mask else B(wrap.mc.dataWidth bit, default -> true)
      this.io.WEMA <> wrap.io.apa.mask
      this.io.WEMB <> wrap.io.apb.mask
      //    }
      this.io.WEA <> wrap.io.dpa.writeEnable
      this.io.WEB <> wrap.io.dpb.writeEnable
      this.io.MEA <> wrap.io.apa.memoryEnable
      this.io.MEB <> wrap.io.apb.memoryEnable
      this.io.TEST1A := False
      this.io.RMEA := False
      this.io.RMA := B"4'b0010"
      this.io.TEST1B := False
      this.io.RMEB := False
      this.io.RMB := B"4'b0010"
      this.io.LS := False
      this
    }

    noIoPrefix()
  }

  class mbbrom(wrap: Rom) extends RomBB(wrap.mc) {
    //  this.setDefinitionName()
    val io = new Bundle {
      val CLK = in Bool()
      val ADR = in UInt (wrap.mc.addrWidth bit)
      val Q = out Bits (wrap.mc.dataWidth bit)
      val ME, LS = in Bool()
      val TEST1, RME = in Bool()
      val RM = in Bits (4 bit)
    }

    val cd = ClockDomain(io.CLK)

    def connectPort(): MemBlackBox = {
      wrap.clockDomain.setSynchronousWith(cd)
      io.CLK <> wrap.clockDomain.readClockWire
      io.ADR <> wrap.io.addr
      io.Q <> wrap.io.rdata
      io.ME <> wrap.io.cs
      io.LS := False
      io.TEST1 := False
      io.RME := False
      io.RM := B"4'b0010"
      this
    }

    noIoPrefix()
  }
}
