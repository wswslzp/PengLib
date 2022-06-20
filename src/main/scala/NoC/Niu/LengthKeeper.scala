package NoC.Niu
import language.postfixOps
import spinal.core._
import spinal.lib._
import Util._

case class LengthKeeper(maxLength: Int) extends Module {
  private val width = log2Up(maxLength)
  val io = new Bundle {
    val input = slave(Stream(UInt(width bit)))
    val flitEnd = in Bool() // flitEnd
    val burstEnd = in Bool() // burstEnd && push
    val output = master(Stream(UInt(width bit)))
    val testID = in UInt(width bit)
  }

//  val halt = Bool()
//  val input1 = io.input.haltWhen(halt).stage()
//  val different = input1.payload =/= io.input.payload
//
//  val stallBegin = different && RegNext(io.burstEnd, False)
//  val stall = RegInit(False).setWhen(stallBegin).clearWhen(io.flitEnd)
//  halt := stall
//
//  io.output << input1

  val length = RegInit(U(0, width bit))
  when(io.input.valid) {
    
  }
}

object LengthKeeper {
  def main(args: Array[String]): Unit = {
    PrintRTL("rtl")(LengthKeeper(32))
  }
}
