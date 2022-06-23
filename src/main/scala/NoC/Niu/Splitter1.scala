package NoC.Niu

import scala.language.postfixOps

import spinal.core._
import spinal.lib._
import fsm._
import Util._

case class Splitter1(maxLength: Int, inputWidth: Int, outputWidth: Int) extends Module {
  private val idWidth = log2Up(maxLength)
  val io = new Bundle {
    val input = slave(Stream(Bits(inputWidth bit)))
    val burstLen = slave(Stream(UInt(idWidth bit)))
    val output = master(Stream(Bits(outputWidth bit)))
    val outputID = out UInt (log2Up(maxLength) bit)
  }

  // input buffer
  val queueInput = io.input.queue(2)
  val queueLen = io.burstLen.queue(2)

  // length keeper
  val keptLen = queueLen.valid ? queueLen.payload | U(0, queueLen.payload.getBitsWidth bit)

  // todo: heavy calculation
  val totalIW = keptLen * inputWidth
  val length1 = totalIW / outputWidth
  val length2 = (totalIW % outputWidth =/= 0).asUInt.resized
  val length = length1 + length2

  // width adapter
  val paddedInputWidth = inputWidth + outputWidth
  val inBuffer = Reg(Bits(paddedInputWidth bit)) init 0
  val bufferCap = Reg(UInt(log2Up(paddedInputWidth)+1 bit)) init 0
  val push = queueInput.fire // push to inner buffer
  val pop = io.output.fire // pop from inner buffer
  val empty, full = Bool()
  empty := bufferCap < outputWidth
  full := bufferCap > (paddedInputWidth - inputWidth)

  io.output.payload := inBuffer(outputWidth-1 downto 0)

  // record the inner buffer capacity
  when(pop && push) {
    bufferCap := bufferCap + inputWidth - outputWidth
  } elsewhen(pop && !push) {
    bufferCap := bufferCap - outputWidth
  } elsewhen(push && !pop) {
    bufferCap := bufferCap + inputWidth
  }

  // assign to inner buffer or shift out data from inner buffer
  val shiftedBuffer = inBuffer |>> outputWidth
  val inBuf = List.tabulate(paddedInputWidth - inputWidth + 1)(i=> inBuffer(i until (i + inputWidth)))
  val shftBuf = List.tabulate(shiftedBuffer.getBitsWidth)(i=> shiftedBuffer(0 until i))
  val shftIndex = (bufferCap+inputWidth-outputWidth).resize(log2Up(shftBuf.length))
  when(pop && !push){ // only shift out
    inBuffer := shiftedBuffer
  }elsewhen(push && !pop){ // only assign some bits
    whenIndexed(inBuf, bufferCap.resize(log2Up(inBuf.length))){bufSlice=>
      bufSlice := queueInput.payload
    }
  }elsewhen(push && pop){ // shift out and assign
    whenIndexed(shftBuf, shftIndex){bufSlice=>
      inBuffer := B(queueInput.payload, bufSlice).resized
    }
  }

  /**
   * indicate whether packet in buffer is clear, will keep true until not clear
   * set when the first push happen and clear when flitEnd set.
   */
  val bufHasPacket = Reg(Bool())

  // id cnt
  val idCounter = RegInit(U(0, idWidth bit))
  val idCounterFull = idCounter === (length - 1)
  val flitEnd = idCounterFull && pop
  when(pop && !idCounterFull && bufHasPacket){
    idCounter := idCounter + 1
  }elsewhen(flitEnd) {
    idCounter := 0
  }

  // clear buffer capacity
  when(flitEnd){
    bufferCap := 0
  }

  io.outputID := idCounter

  // burst cnt
  val burstCnt = Reg(keptLen) init 0
  val burstEnd = burstCnt === (keptLen - 1)
  when(push && !burstEnd) {
    burstCnt := burstCnt + 1
  } elsewhen(burstEnd && push) {
    burstCnt := 0
  }

  val packetBegin = burstCnt === 0 && push
  bufHasPacket.setWhen(packetBegin).clearWhen(flitEnd)
  io.output.valid := (!empty || idCounterFull) && bufHasPacket

  // stall the queue pop port if next packet will come out but the current packet at output port isn't
  // finished.
  val packetEventRange = RegInit(False).setWhen(packetBegin).clearWhen(flitEnd)
  queueLen.ready := flitEnd
  queueInput.ready := !full && !idCounterFull && queueLen.valid

}

object Splitter1 {
  def main(args: Array[String]): Unit = {
    import Util._
    PrintRTL("rtl1")(Splitter1(32, 12, 8)).printRtl()
  }
}
