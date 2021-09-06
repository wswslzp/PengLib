package InterpolatePack

import spinal.core._
import spinal.lib._

case class SingleParamValuePair[T <: Data with Num[T]](dataType: HardType[T]) extends Bundle{
  val param = dataType()
  val value = dataType()
}

/**
 * define the basic interface for the interpolate unit.
 * @param cfg Configuration of BIU
 * @tparam T Data type that has basic arithmetic operations.
 */
sealed class BasicInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends Component {
  require(cfg.ports >= 2)
  val paramValuesIo = in( Vec.fill(cfg.ports)(SingleParamValuePair(cfg.dataType)) )
  val io = new Bundle {
    val x  = in(cfg.dataType())
    val y  = out(cfg.dataType())
  }

}

class NearestBIU[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends BasicInterpolateUnit[T](cfg) {
  val pv0 = paramValuesIo(0)
  val pv1 = paramValuesIo(1)
  val x_interval = pv1.param - pv0.param
  val x_diff = io.x - pv0.param
  val v_sel = x_diff <= (x_interval >> 1)
  io.y := v_sel ? pv0.value | pv1.value
}

class LinearBIU[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends BasicInterpolateUnit[T](cfg) {
  val pv0 = paramValuesIo(0)
  val pv1 = paramValuesIo(1)
  val x_interval = pv1.param - pv0.param
  val x_diff = io.x - pv0.param
  val f_interval = pv1.value - pv0.value
  val xf_prod = f_interval * x_diff
  io.y := (xf_prod/x_interval + pv0.value).resized // todo: bit width truncation implement
}

class CubicBIU[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends BasicInterpolateUnit[T](cfg) {

}

//class FractionalBIU[T <: Data with Num[T]](dataType: HardType[T], ports: Int) extends BasicInterpolateUnit[T](dataType, ports){
//
//}
