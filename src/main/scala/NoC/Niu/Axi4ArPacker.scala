package NoC.Niu

import NoC.{Flit, NodeID}
import spinal.core._
import spinal.lib.bus.amba4.axi._
import spinal.lib._

case class Axi4ArPacker[T <: Data](config: NiuConfig[T], axi4Config: Axi4Config) extends Module {
  private val PackerConfig(packetType, flitConfig, dt) = config.packerConfig
  val io = new Bundle {
    val ar = slave(Stream(Axi4Ar(axi4Config)))
    val flit = master(Stream(Flit(flitConfig, dt())))
  }
  noIoPrefix()
  val payload = io.ar.payload

  // id remapper
  val pID = payload.id
  val pType = packetType

  // routing table
  // input address, output a target id
  // also generate source id
  val srcID = NodeID(config.x, config.y, flitConfig)
  val tgtID = config.routingTable(payload.addr) // todo: where should the routing table function be?

  // payload splitter
  val pLen = payload.getBitsWidth / io.flit.payload.getBitsWidth // todo check if divide

}



