package InterpolatePack

import spinal.core._
import spinal.lib._

import scala.beans.BeanProperty

case class InterpolateConfig[T <: Data with Num[T]](
                                                   dataType: HardType[T],
                                                   dim: Int,
                                                   ){
  @BeanProperty
  var ports: Int = 2
}
