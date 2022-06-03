package NoC.Mesh

import NoC._

import scala.language._
import spinal.core._

/**
 * Router with mesh topology routing algorithm
 * @tparam T data type
 */
trait MeshType[T <: Data] { this: RouterBase[T] =>
  import MeshType._
  def localX: UInt
  def localY: UInt
  def toLocal(attribute: FlitAttribute): Bool = attribute.targetID.x === localX && attribute.targetID.y === localY

  def crossRouteX(index: Int)(attr: FlitAttribute): Bool = {
    val ret = Bool()
    when(attr.targetID.x === localX) {
      ret.set()
    } elsewhen (attr.targetID.x > localX) {
      ret := Bool(index == SOUTH) //3
    } otherwise {
      ret := Bool(index == NORTH) //2
    }
    ret
  }
  def crossRouteY(index: Int)(attr: FlitAttribute): Bool = {
    val ret = Bool()
    when(attr.targetID.x =/= localX) {
      ret.set()
    } elsewhen (attr.targetID.y > localY) {
      ret := Bool(index == EAST) // 0
    } otherwise {
      ret := Bool(index == WEST) // 1
    }
    ret
  }
}

object MeshType {
  def EAST = 0
  def WEST = 1
  def NORTH = 2
  def SOUTH = 3
}
