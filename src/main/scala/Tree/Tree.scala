package Tree

import MathLib.Interpolate.{BasicInterpolateUnit, InterpolateUnit, SinglePoint}
import spinal.core._
import spinal.lib._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
sealed trait Tree[+A]

class IUTreeNode[T <: Data with Num[T]](iu: InterpolateUnit[T]) extends Tree[InterpolateUnit[T]]{
  val pointPerDim = iu.getPointPerDim
  var unit: BasicInterpolateUnit[T] = _

  var father: IUTreeNode[T] = _
  var son: List[IUTreeNode[T]] = _
  var level: Int = _

  // The basic interpolation units consists of a tree structure
  // It's depth is equals to the dim-1
  // and every node of the tree has up to pointPerDim sons/leaves
  // Each node contain a basic interpolation unit.
  def createSon(lv: Int): Unit = {
    if(lv >= 0){
      level = lv
      unit = iu.getIU(lv)
      son = List.fill(pointPerDim(lv))(new IUTreeNode[T](iu))
      son.foreach{s=>
        s.father = this
        s.createSon(lv-1)
      }
    }
  }
  def isRoot = father == null
  def isLeaf = son.forall(_.unit == null)
  def index = father.son.indexOf(this)

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
  def getNodePvPort(idx: Int): SinglePoint[T] = unit.paramValuesIo(idx)
}

class IUTree[T <: Data with Num[T]](iu: InterpolateUnit[T]) extends Tree[InterpolateUnit[T]]{
  val dim = iu.getDim
  val dataType: HardType[T] = iu.getDataType
  val pointPerDim = iu.getPointPerDim

  // Need to setup during the IU.build()
  lazy val inputVector = ArrayBuffer.fill(dim)(dataType())
  lazy val paramVector = List.tabulate(dim)(d=>
    ArrayBuffer.fill(pointPerDim(d))(dataType())
  )
//  lazy val valueVector = List.fill(scala.math.pow(pointPerDim, dim-1).toInt)(
//    ArrayBuffer.fill(pointPerDim)(dataType())
//  )
  lazy val valueVector = ArrayBuffer.fill(pointPerDim.product)(dataType())

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
  private def simpleTraverseTree(f: IUTreeNode[T] => Unit): Unit = traverseTree(f)(nodelist=> nodelist.foreach(f(_)))
  private def giveIuName(): Unit = simpleTraverseTree{leaf=>
    val name = leaf.unit.getName()
    val level = leaf.level
    val index = leaf.index
    leaf.unit.setName(name + s"_lv$level" + s"_id$index")
  }

  /**
   * Connect the sons' output to the fathers' input.
   */
  def connectUnits(): Unit = {
    var leaf_idx = 0
    traverseTree{leaf: IUTreeNode[T] =>
      // connect input to the leaves
      val leafDim = leaf.level
      leaf.getNodeInput := inputVector(leafDim)
      for(i <- 0 until pointPerDim(leafDim)){
        val sp = SinglePoint(dataType())
        sp.param := paramVector(leafDim)(i)
        sp.value := valueVector(i + leaf_idx * pointPerDim(leafDim))
//        println(s"connecting leaf pv($leaf_idx)($i) to param($leafDim)($i) and value(${i + leaf_idx * pointPerDim(leafDim)})")
        leaf.getNodePvPort(i) := sp
      }
      leaf_idx += 1
    }{sons=>
      // connect input to the fathers
      val father = sons.head.father
      val downLevel = father.level
      father.getNodeInput := inputVector(downLevel)
      // connect the parameters and values from sons to fathers
      for(i <- 0 until pointPerDim(downLevel)){
        val sp = SinglePoint(dataType())
        sp.param := paramVector(downLevel)(i)
        sp.value := sons(i).getNodeOutput
        father.getNodePvPort(i) := sp
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
