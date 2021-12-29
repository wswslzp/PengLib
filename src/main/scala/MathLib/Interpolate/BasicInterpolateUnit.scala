package MathLib.Interpolate

import spinal.core._
import spinal.lib._

case class SingleParamValuePair[T <: Data with Num[T]](dataType: HardType[T]) extends Bundle{
  val param = dataType()
  val value = dataType()
}

/**
 * define the basic interface for the interpolate unit, which evaluate on a single dimension.
 * @param cfg Configuration of BIU
 * @tparam T Data type that has basic arithmetic operations.
 */
sealed abstract class BasicInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends Component {
  require(cfg.pointPerDim >= 2)
  val paramValuesIo = in( Vec.fill(cfg.pointPerDim)(SingleParamValuePair(cfg.dataType)) )
  val io = new Bundle {
    val x  = in(cfg.dataType())
    val y  = out(cfg.dataType())
  }

  /**
   * Find the interval that x located in.
   * @return
   */
  def findInterval() = new Area {
    setName("compare")
    val params = paramValuesIo.map(_.param)
    val compared = params.map(io.x < _)
    val pv0Index = CountOne(compared)
    val pv1Index = pv0Index - 1
    val pvPair = paramValuesIo(pv0Index.resized) -> paramValuesIo(pv1Index.resized)
  }.pvPair

  /**
   * Implement the evaluation
   * @param pv0 - lower bound
   * @param pv1 - upper bound
   */
  def evaluate(pv0: SingleParamValuePair[T], pv1: SingleParamValuePair[T]): Unit

  afterElaboration {
    val pvPair = findInterval()
    evaluate(pvPair._1, pvPair._2)
  }
}

/**
 * @param cfg Configuration of BIU
 * @tparam T Data type that has basic arithmetic operations.
 */
class NearestBIU[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends BasicInterpolateUnit[T](cfg) {
  override def evaluate(pv0: SingleParamValuePair[T], pv1: SingleParamValuePair[T]): Unit = new Area {
    val x_interval = pv1.param - pv0.param
    val x_diff = io.x - pv0.param
    val v_sel = x_diff <= (x_interval >> 1)
    io.y := v_sel ? pv0.value | pv1.value
  }.setName("eval")
}

class LinearBIU[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends BasicInterpolateUnit[T](cfg) {
  override def evaluate(pv0: SingleParamValuePair[T], pv1: SingleParamValuePair[T]): Unit = new Area {
    val x_interval = pv1.param - pv0.param
    val x_diff = io.x - pv0.param
    val f_interval = pv1.value - pv0.value
    val xf_prod = f_interval * x_diff
    io.y := (xf_prod/x_interval + pv0.value).resized // todo: bit width truncation implement
  }.setName("eval")
}

/**
 * For cubic implement, the interval is different from the two above.
 * @param cfg Configuration of BIU
 * @tparam T Data type that has basic arithmetic operations.
 */
class CubicBIU[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends BasicInterpolateUnit[T](cfg) {
  override def evaluate(pv0: SingleParamValuePair[T], pv1: SingleParamValuePair[T]): Unit = ???
}

//class FractionalBIU[T <: Data with Num[T]](dataType: HardType[T], ports: Int) extends BasicInterpolateUnit[T](dataType, ports){
//
//}
