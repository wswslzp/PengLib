package MathLib.Interpolate

import spinal.core._

import scala.beans.BeanProperty
import scala.collection.mutable

case class InterpolateConfig[T <: Data with Num[T]](
                                                     dataType: HardType[T],
                                                     totalDim: Int,
                                                   ) {

  val diffPointPerDim = mutable.ArrayBuffer[Int]()

  def setPointPerDim(ppd: Seq[Int]): Unit = {
    diffPointPerDim.clear()
    diffPointPerDim ++= ppd
  }

  def coordinateToIndex(c: Seq[Int]): Int = {
    require(c.length == diffPointPerDim.length, s"Provided coordinate ${c.toString()} has different dimension from the config.")
    val checkBoundary = c.view.zip(diffPointPerDim).map(p => p._1 < p._2).reduce(_ && _)
    require(checkBoundary, s"Provided coordinate ${c.toString()} exceed the boundary of the grid.")
    var ret = diffPointPerDim.head
    for(d <- 1 until totalDim){
      ret += c(d) * diffPointPerDim.take(d).product
    }
    ret
  }

  def indexToCoordinate(ind: Int): Vector[Int] = {
    require(ind < diffPointPerDim.product, s"Given index $ind greater than the maximum index ${diffPointPerDim.product}")
    val ret = Vector.newBuilder[Int]
    for(d <- 0 until totalDim){
      ret += ind % diffPointPerDim.take(d+1).product
    }
    ret.result()
  }
}
