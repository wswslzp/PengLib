package NoC.Niu
import scala.language.postfixOps

import spinal.core._
import spinal.lib._

import Util._

case class PayloadWithLength(maxLength: Int, dataWidth: Int) extends Bundle {
  val data = Bits(dataWidth bit)
  val length = UInt(log2Up(maxLength) bit)

  def withFlag(flag: Bool): PayloadWithLengthAndFlag = {
    val ret = PayloadWithLengthAndFlag(maxLength, dataWidth)
    ret.data := data
    ret.length := length
    ret.flag := flag
    ret
  }
}

case class PayloadWithLengthAndFlag(maxLength: Int, dataWidth: Int) extends Bundle {
  val flag = Bool()
  val data = Bits(dataWidth bit)
  val length = UInt(log2Up(maxLength) bit)
}

/**
 * Flit splitter.
 * @param maxLength
 * @param inputWidth
 * @param outputWidth
 */
case class Splitter(maxLength: Int, inputWidth: Int, outputWidth: Int) extends Module {
  private val idWidth = log2Up(maxLength)
  val io = new Bundle {
    val input = slave(Stream(PayloadWithLength(maxLength, inputWidth)))
    val output = master(Stream(Bits(outputWidth bit)))
    val outputID = out UInt (log2Up(maxLength) bit)
  }
  noIoPrefix()

  val flag = RegInit(False)
  val input1 = io.input.stage()
  val inputValidRise = io.input.valid.rise(False)
  when(inputValidRise) {flag := !flag}

  val input2 = input1.map(_.withFlag(flag))

  // input buffer
//  val queueInput = io.input.queue(2)
  val queueInput = input2.queue(2)

  // todo: heavy calculation
  val totalIW = queueInput.length * inputWidth
  val length1 = totalIW / outputWidth
  val length2 = (totalIW % outputWidth =/= 0).asUInt.resized
  val length = length1 + length2 // todo, length should not be changed if current packet isn't finished.

  // width adapter
  val factor = (inputWidth + outputWidth - 1) / outputWidth
  val paddedInputWidth = factor * outputWidth
  val inBuffer = Reg(Bits(paddedInputWidth bit)) init 0
  val bufferCap = Reg(UInt(log2Up(paddedInputWidth)+1 bit)) init 0
  val push = queueInput.fire // push to inner buffer
  val pop = io.output.fire // pop from inner buffer
  val empty, full = Bool()
  empty := bufferCap < outputWidth
  full := bufferCap > (paddedInputWidth - inputWidth)

  io.output.payload := inBuffer(outputWidth-1 downto 0)

//  queueInput.ready := !full

  when(pop && push) {
    bufferCap := bufferCap + inputWidth - outputWidth
  } elsewhen(pop && !push) {
    bufferCap := bufferCap - outputWidth
  } elsewhen(push && !pop) {
    bufferCap := bufferCap + inputWidth
  }

  val inBuf = List.tabulate(paddedInputWidth - inputWidth + 1)(i=> inBuffer(i until (i + inputWidth)))
  val inBufIndex = bufferCap - inputWidth
  whenIndexed(inBuf, bufferCap.resize(log2Up(paddedInputWidth - inputWidth + 1))){bufSlice=>
    when(push){
      bufSlice := queueInput.data
    }
  }
  when(pop){
    inBuffer := inBuffer |>> outputWidth
  }

  /**
   * indicate whether packet in buffer is clear, will keep true until not clear
   * set when the first push happen and clear when flitEnd set.
   */
  val bufHasPacket = Reg(Bool())

  // id
  val idCounter = RegInit(U(0, idWidth bit))
  val idCounterFull = idCounter === (length - 1)
  val flitEnd = idCounterFull && pop
  when(pop && !idCounterFull && bufHasPacket){
    idCounter := idCounter + 1
  }elsewhen(flitEnd) {
    idCounter := 0
  }
  io.outputID := idCounter

  // burst cnt
  val burstCnt = Reg(queueInput.length) init 0
  val burstEnd = burstCnt === (queueInput.length - 1)
  when(push && !burstEnd) {
    burstCnt := burstCnt + 1
  } elsewhen(burstEnd && push) {
    burstCnt := 0
  }

  // clear buffer capacity
  val packetBegin = burstCnt === 0 && push
  bufHasPacket.setWhen(packetBegin).clearWhen(flitEnd)
  io.output.valid := (!empty || idCounterFull) && bufHasPacket
  val flag1 = RegInit(False)
  when(flitEnd){
    bufferCap := 0
    flag1 := !flag1
  }

  // stall the queue pop port if next packet will come out but the current packet at output port isn't
  // finished.
  val packetEventRange = RegInit(False).setWhen(packetBegin).clearWhen(flitEnd) // from begin to end
  when(packetBegin) {flag1 := queueInput.flag} // todo
  val tmp = !((flag1 =/= queueInput.flag) && packetEventRange)
  queueInput.ready := !full //&& !((flag1 =/= queueInput.flag) && packetEventRange) // todo

}

object Splitter {
  def main(args: Array[String]): Unit = {
    import Util._
    PrintRTL("rtl1")(Splitter(32, 12, 8))
  }
}
