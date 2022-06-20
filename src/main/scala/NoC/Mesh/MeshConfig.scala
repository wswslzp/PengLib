package NoC.Mesh

import scala.language._
import spinal.core._
import NoC._
import spinal.lib._
import spinal.lib.bus.misc.SizeMapping

import scala.collection.mutable

case class MeshConfig[T <: Data](routerConfig: RouterConfig[T], xNum: Int, yNum: Int) {
  /**
   * key: address map, value: node id
   */
  val addressTable = mutable.Map[SizeMapping, (Int, Int)]()

  def route(address: UInt): NodeID = {
    val addressHit = addressTable.keys.map(_.hit(address))
    val nodeIDs = addressTable.values.map(pos=> NodeID(pos._1, pos._2, routerConfig.flitConfig))
    Vec(nodeIDs).oneHotAccess(addressHit.asBits())
  }
}
