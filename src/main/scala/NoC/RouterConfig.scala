package NoC

import spinal.core._
import spinal.lib._

import scala.language._

case class RouterConfig[T<:Data](flitConfig: FlitConfig, dataType: HardType[T], portNum: Int = 4) {
  require(portNum == 1 || portNum == 2 || portNum == 4)
}

case class MeshChannel[T <: Data](routerConfig: RouterConfig[T]) extends Bundle {
  val input = slave Flow Flit(routerConfig.flitConfig, routerConfig.dataType)
  val output = master Flow Flit(routerConfig.flitConfig, routerConfig.dataType)
}

case class RequestChannel[T <: Data](routerConfig: RouterConfig[T]) extends Bundle {
  val input = slave Stream Flit(routerConfig.flitConfig, routerConfig.dataType)
  val output = master Stream Flit(routerConfig.flitConfig, routerConfig.dataType)
}

