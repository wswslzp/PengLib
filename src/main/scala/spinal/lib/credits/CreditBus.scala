package spinal.lib.credits

import spinal.core._
import spinal.lib._
import tools._

case class CreditBus[T <: Data](payloadType: HardType[T]) extends Bundle with IMasterSlave{ self=>
  import CreditBus._
  val payload = payloadType()
  val valid = Bool()
  val credit = Bool()

  override def asMaster(): Unit = {
    out(payload, valid)
    in(credit)
  }

  def asFlow: Flow[T] = {
    val ret = Flow(payloadType())
    ret.valid := valid
    ret.payload := payload
    ret
  }

  def stage(): CreditBus[T] = {
    val ret = CreditBus(payloadType())
    ret.payload := RegNext(payload)
    ret.valid := RegNext(valid) init False
    credit := RegNext(ret.credit) init False
    ret
  }

  def pipe(n: Int): CreditBus[T] = {
    val pipeline = List.fill(n)(CreditBus(payloadType))
    pipeline.fold(this)((a, b)=> a >-> b)
  }

  def <<(that: CreditBus[T]): CreditBus[T] = {
    valid := that.valid
    payload := that.payload
    that.credit := credit
    that
  }

  def >>(that: CreditBus[T]): CreditBus[T] = {
    that << this
    that
  }

  def <-<(that: CreditBus[T]): CreditBus[T] = this << that.stage()
  def </<(that: CreditBus[T]): CreditBus[T] = this << that.stage()

  def >->(that: CreditBus[T]): CreditBus[T] = this.stage() >> that
  def >/>(that: CreditBus[T]): CreditBus[T] = this.stage() >> that

  def toStream(maxCredit: Int): Stream[T] = new Composite(this) {
    val begin = searchBegin(valid) // search begin valid signal
    var chosenCreditNum = 0
    if (begin.isDefined) {
      busDict(begin.get) = Some(valid)
      latencyCompute()
      chosenCreditNum = Math.max(creditNumbers(begin.get), maxCredit)
      begin.get.component.asInstanceOf[StreamToCreditBus[T]].reworkCreditsNumber(chosenCreditNum)
    } else {
      chosenCreditNum = maxCredit
    }
    val adapter = CreditBusToStream(payloadType, chosenCreditNum)
    adapter.io.up << self
  }.adapter.io.down

  def arbitrateFrom[T1 <: Data](that: CreditBus[T1]): Unit = {
    this.valid := that.valid
    that.credit := this.credit
  }

  def translateWith[T1 <: Data](that: T1): CreditBus[T1] = {
    val ret = CreditBus(that)
    ret.arbitrateFrom(this)
    ret.payload := that
    ret
  }

  def map[T1 <: Data](func: T=> T1): CreditBus[T1] = this.translateWith(func(payload))
  def ~~[T1 <: Data](func: T=> T1): CreditBus[T1] = this.translateWith(func(payload))

  case class CreditPipe(bus: CreditBus[T], n: Int) {
    require(n > 1, "The pipe stage number is at least 2.")
    def <(that: CreditBus[T]): CreditBus[T] = bus << that.pipe(8)
    def >(that: CreditBus[T]): CreditBus[T] = bus >> that.pipe(8)
  }
  def <(n: Int): CreditPipe = CreditPipe(this, n)
  def >(n: Int): CreditPipe = CreditPipe(this, n)
}

object CreditBus extends CreditFactory {
  import scala.collection.mutable

  val busDict = mutable.LinkedHashMap[BaseType, Option[BaseType]]()
  val latency = mutable.LinkedHashMap[BaseType, Int]()
  val creditNumbers = mutable.LinkedHashMap[BaseType, Int]()

  def latencyCompute(): Unit = {
    busDict.foreach {case (a, b) =>
      if (b.isDefined) latency(a) = LatencyAnalysis(a, b.get)
      else latency(a) = 7
      creditNumbers(a) = Math.max(creditNumbers(a), 2 * latency(a))
    }
  }

  def searchBegin(bt: BaseType): Option[BaseType] = {
    import DataAnalyzer._
    val fanin = bt.allFanIn
    if (fanin.isEmpty) {
      None
    } else {
      fanin.map {fi=>
        if (busDict.keySet.contains(fi)) {
          Some(fi)
        } else {
          searchBegin(fi)
        }
      }.filter(_.isDefined).head
    }
  }

  def driveFrom[T <: Data](bus: Stream[T], maxCreditNum: Int, resetCreditNum: Int = 1): CreditBus[T] = new Composite(bus) {
    val adapter = StreamToCreditBus(maxCreditNum = maxCreditNum, payloadType = bus.payload, resetCreditNum = resetCreditNum)
    adapter.io.up << bus
    busDict(adapter.io.down.valid) = None
    creditNumbers(adapter.io.down.valid) = maxCreditNum
  }.adapter.io.down
}
