package NoC

import scala.language._
import spinal.core._

abstract class RouterBase[T <: Data](config: RouterConfig[T]) extends Module with RenameFlitPayload {
  val io = new Bundle {
    val meshIO = Array.fill(config.portNum)(MeshChannel(config))
    val reqIO = Array.fill(config.portNum)(RequestChannel(config))
  }

  def assignNext(that: RouterBase[T], from: Int, to: Int): Unit = {
    that.io.meshIO(to).input << io.meshIO(from).output
    that.io.meshIO(to).output >> io.meshIO(from).input
  }
  def assignNext(that: RouterBase[T])(fromTo: (Int, Int)): Unit = assignNext(that, fromTo._1, fromTo._2)

  def shortConnectOn(port: Int): Unit = {
    io.meshIO(port).input << io.meshIO(port).output
  }

  noIoPrefix()
}
