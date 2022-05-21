package NoC

import scala.language._
import spinal.core._

abstract class Router[T <: Data](config: RouterConfig[T]) extends Module with RenameFlitPayload {
  val io = new Bundle {
    val meshIO = Array.fill(config.portNum)(MeshChannel(config))
    val reqIO = Array.fill(config.portNum)(RequestChannel(config))
  }
  noIoPrefix()
}
