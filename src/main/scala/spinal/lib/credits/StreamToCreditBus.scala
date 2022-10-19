package spinal.lib.credits

import spinal.core._
import spinal.lib._
import fiber._
import spinal.core.internals.PhasePullClockDomains

case class StreamToCreditBus[T<:Data](payloadType: HardType[T], maxCreditNum: Int, resetCreditNum: Int = 1) extends Module {
  val io = new Bundle {
    val up = slave(Stream(payloadType()))
    val down = master(CreditBus(payloadType()))
    val isIdle = out Bool()
  }

  def gen(creditNum: Int): Unit = new AreaRoot {
    val counterMax = 1 << log2Up(creditNum)
    val credit = CounterUpDown(counterMax * 2)
    credit.init(resetCreditNum)

    io.up.ready := credit =/= 0
    val iFire = io.up.fire
    val data = RegNextWhen(io.up.payload, iFire)
    io.down.payload := data
    io.down.valid := RegNext(iFire)

    val downCredit_r = RegNext(io.down.credit)
    switch(iFire ## downCredit_r) {
      is(B"2'b01") {
        credit.increment()
      }
      is(B"2'b10") {
        credit.decrement()
      }
    }

    io.isIdle := credit.willOverflowIfInc
  }

  gen(maxCreditNum)

  def reworkCreditsNumber(creditNum: Int): Unit = {
    rework {
      // step0: walk and fix clock (clock, reset port need keep)
      PhasePullClockDomains.recursive(this)
      // step1: First remove all we don't want
      this.children.clear()
      this.dslBody.foreachStatements{
        case bt : BaseType if !bt.isDirectionLess =>
        case s => s.removeStatement()
      }
      // step2: remove output and assign zero
      // this step can't merge into step1
      this.dslBody.foreachStatements{
        case bt : BaseType if bt.isOutput | bt.isInOut =>
          bt.removeAssignments()
        case s =>
      }

      gen(creditNum)
    }
  }

}

object StreamToCreditBus {
  def main(args: Array[String]): Unit = {
    import Util._
    PrintRTL("rtl")(StreamToCreditBus(Bits(32 bit), 16)).printRtl()
  }
}
