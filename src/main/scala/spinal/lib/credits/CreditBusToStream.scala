package spinal.lib.credits


import spinal.core._
import spinal.lib._

case class CreditBusToStream[T<:Data](payloadType: HardType[T], maxCreditNum: Int) extends Module {
  val io = new Bundle {
    val down = master(Stream(payloadType()))
    val up = slave(CreditBus(payloadType()))
    val isFull = out Bool()
  }

  val up = io.up.stage()
  up.addAttribute(new AttributeFlag("sss", COMMENT_ATTRIBUTE))

  val buffer = StreamFifo(payloadType(), 1 << log2Up(maxCreditNum))

  buffer.io.push.valid := up.valid
  buffer.io.push.payload := up.payload
  io.isFull := !buffer.io.push.ready

  up.credit := buffer.io.pop.fire

  io.down.payload := buffer.io.pop.payload
  io.down.valid := buffer.io.pop.valid
  buffer.io.pop.ready := io.down.ready
}

object CreditBusToStream {
  def main(args: Array[String]): Unit = {
    import Util._
    PrintRTL("rtl")(CreditBusToStream(Bits(32 bit), 16)).printRtl()
  }
}
