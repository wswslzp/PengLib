package NoC

import scala.language.postfixOps
import spinal.core._
import spinal.lib._

case class NodeID(config: FlitConfig) extends Bundle {
  val x,y = UInt(config.dirWidth bit)
}
object NodeID {
  def apply(x: Int, y: Int, config: FlitConfig): NodeID = {
    val ret = NodeID(config)
    ret.x := x
    ret.y := y
    ret
  }
}

object PacketType extends SpinalEnum {
  val AR, R, AW, W, B = newElement()
}

case class FlitAttribute(config: FlitConfig) extends Bundle {
  val packetID = Bits(config.packetIdWidth bit)
  val packetType = PacketType()
  val packetLen = UInt(config.packetLenWidth bit)
  val flitID = Bits(config.flitIdWidth bit)
  val sourceID, targetID = NodeID(config)
  val txnID = ifGen(config.useTxn)(Bits(config.txnIdWidth bit))
  val qos = ifGen(config.useQos)(Bits(config.qosWidth bit))
  val mode = ifGen(config.useMode)(Bits(config.modeWidth bit))
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