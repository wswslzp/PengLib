package NoC.Niu
import NoC._
import spinal.core._

case class PackerConfig[T<:Data](
                       packetType: Int,
                       flitConfig: FlitConfig,
                       flitPayload: HardType[T]
                       )

case class NiuConfig[T<:Data](
                              x: Int, y: Int,
                              packerConfig: PackerConfig[T],
                              routingTable: UInt => NodeID
                             )