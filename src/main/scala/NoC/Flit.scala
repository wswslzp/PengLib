package NoC

import scala.language.postfixOps
import spinal.core._
import spinal.lib._

case class NodeID(config: FlitConfig) extends Bundle {
  val x,y = UInt(config.dirWidth bit)
}

case class FlitAttribute(config: FlitConfig) extends Bundle {
  val qos = Bits(config.qosWidth bit)
  val mode = Bits(config.modeWidth bit)
  val sourceID, targetID = NodeID(config)
  val txnID = Bits(config.txnIdWidth bit)

  def targetIDExtended = targetID.x.resize(8) ## targetID.y.resize(8)
}

case class Flit[T <: Data](config: FlitConfig, dataType: HardType[T]) extends Bundle {
  val attribute = FlitAttribute(config)
  val payload = dataType()
}

object Flit {
  def firstSchedule[T<:Data](forward: Flow[T], turn: Stream[T] = null) = new Composite(forward) {
    val ret = cloneOf(forward)
    ret << forward
    if(turn != null) {
      turn.ready := !forward.valid
      when(!forward.valid) {
        ret << turn.toFlowFire
      }
    }
  }.ret
}