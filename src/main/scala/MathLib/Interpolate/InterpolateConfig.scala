package MathLib.Interpolate

import spinal.core._

import scala.beans.BeanProperty

case class InterpolateConfig[T <: Data with Num[T]](
                                                     dataType: HardType[T],
                                                     dim: Int,
                                                   ) {
  @BeanProperty
  var pointPerDim: Int = 2
}
