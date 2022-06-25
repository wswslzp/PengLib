package NoC.Niu
import NoC.Mesh._
import scala.language.postfixOps
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.SizeMapping

case class RoutingTable[T<:Data](x: Int, y: Int, addrWidth: Int, meshConfig: MeshConfig[T]) extends Module {
  private val rc = meshConfig.routerConfig
  private val fc = rc.flitConfig
  val io = new Bundle {
    val address = in UInt(addrWidth bit)
    val srcId, tgtId = out(NoC.NodeID(fc))
  }

  io.srcId.x := x
  io.srcId.y := y
  io.tgtId   := meshConfig.route(io.address)
}
