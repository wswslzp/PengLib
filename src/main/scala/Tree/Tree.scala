package Tree

import spinal.core._
import spinal.lib._
import InterpolatePack._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
sealed trait Tree[+A]

class IUTreeNode[T <: Data with Num[T]](iu: InterpolateUnit[T]) extends Tree[InterpolateUnit[T]]{
  val pv_ports = iu.getPortsNum
  var unit: BasicInterpolateUnit[T] = _

  var father: IUTreeNode[T] = _
  var son: List[IUTreeNode[T]] = _
  var level: Int = _

  // software data structure
  def createSon(lv: Int): Unit = {
    if(lv >= 0){
      level = lv
      unit = iu.getIU
      son = List.fill(pv_ports)(new IUTreeNode[T](iu))
      son.foreach{s=>
        s.father = this
        s.createSon(lv-1)
      }
    }
  }
  def isRoot = father == null
  def isLeaf = son.forall(_.unit == null)

  def deepFirstTraverse[B](f: IUTreeNode[T]=>B)(g: List[B] => B): B = {
    if(isLeaf){ f(this) }
    else {
      val gx: List[B] = son.map(s=> s.deepFirstTraverse(f)(g))
      g(gx)
    }
  }

  // hardware part
  def getNodeOutput: T = unit.io.y
  def getNodeInput: T = unit.io.x
  def getNodePvPort(idx: Int): SingleParamValuePair[T] = unit.paramValuesIo(idx)
}

class IUTree[T <: Data with Num[T]](iu: InterpolateUnit[T]) extends Tree[InterpolateUnit[T]]{
  val dim = iu.getDim
  val dataType: HardType[T] = iu.getDataType
  val ports = iu.getPortsNum

  // Need to setup during the IU.build()
  lazy val input_data_vec = ArrayBuffer.fill(dim)(dataType())
  lazy val param_data_vec = List.fill(dim)(
    ArrayBuffer.fill(ports)(dataType())
  )
  lazy val value_data_vec = List.fill(scala.math.pow(ports, dim-1).toInt)(
    ArrayBuffer.fill(ports)(dataType())
  )

  val root = new IUTreeNode(iu)
  root.createSon(dim-1)

  private def traverseTree[B](f: IUTreeNode[T]=> B)(g: List[B]=> B): B = root.deepFirstTraverse(f)(g)
  private def traverseTree(f: IUTreeNode[T] => Unit)(g: List[IUTreeNode[T]]=> Unit): IUTreeNode[T] = {
    def onLeafNode(leaf: IUTreeNode[T]): IUTreeNode[T] = {
      f(leaf)
      leaf
    }
    def onBranchNode(sons: List[IUTreeNode[T]]): IUTreeNode[T] = {
      g(sons)
      sons.head.father
    }
    traverseTree(u=> onLeafNode(u))(s=> onBranchNode(s))
  }

  /**
   * Connect the sons' output to the fathers' input.
   */
  def connectUnits(): Unit = {
    var leaf_idx = 0
    traverseTree{leaf: IUTreeNode[T] =>
      // connect input to the leaves
      leaf.getNodeInput := input_data_vec(leaf.level)
      for(i <- 0 until ports){
        val sgpv = SingleParamValuePair(dataType())
        sgpv.param := param_data_vec(leaf.level)(i)
        sgpv.value := value_data_vec(leaf_idx)(i)
        leaf.getNodePvPort(i) := sgpv
      }
      leaf_idx += 1
    }{sons=>
      // connect input to the fathers
      val father = sons.head.father
      father.getNodeInput := input_data_vec(father.level)
      // connect the parameters and values from sons to fathers
      for(i <- 0 until ports){
        val sgpv = SingleParamValuePair(dataType())
        sgpv.param := param_data_vec(father.level)(i)
        sgpv.value := sons(i).getNodeOutput
        father.getNodePvPort(i) := sgpv
      }
    }
  }

  def getOutput = root.getNodeOutput
}

object IUTree {

  def main(args: Array[String]): Unit = {
//    val a_tree = new IUTree[UInt, NearestInterpolateUnit[UInt]](2, 2, new NearestInterpolateUnit[UInt](UInt(3 bit)))
//    a_tree.dataType = UInt(3 bit)
//
//    println(s"root is leaf? ${a_tree.root.isLeaf}")
//    a_tree.traverseTree{leaf=>
//      println(s"here is leaf")
//      leaf
//    }{sons_eff=>
//      sons_eff.foreach(s=> println(s"current level is ${s.level}"))
//      sons_eff.head.father
//    }
  }
}
