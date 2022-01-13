package MathLib.Interpolate

import spinal.core._

import scala.collection.mutable

trait InterpolatePolicy {
  def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]): InterpolateUnit[T]
}
object Nearest extends InterpolatePolicy{
  override def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]) = new NearestInterpolateUnit[T](cfg)
}
object Linear extends InterpolatePolicy {
  override def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]) = new LinearInterpolateUnit[T](cfg)
}
object Cubic extends InterpolatePolicy {
  override def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]) = new CubicInterpolateUnit[T](cfg)
} // expensive to support. need 3 points to do basic interpolation
//object Fractional extends InterpolatePolicy {
//  override def build(dt: HardType[Interpolate.Fractional.T]) = ???
//}

/**
 * Factory to build the interpolate unit (IU). It has the duty to order the input params and values,
 * send them into the IU, and build the IU according to the chosen interpolation policy.
 * @param paramValues All sorted interpolation points. The order of the points are important to take care.
 * @tparam T unified data type.
 */
class Interpolate[T <: Data with Num[T]](val paramValues: Map[List[T], T]) {
  private val dataType = paramValues.head._2
  private val points = paramValues.toList.length
  private val dim = paramValues.head._1.length
//  private val pointPerDim = (scala.math.log(points)/scala.math.log(dim)).toInt
  require(points >= (1 << dim), "Insufficient points.")
  private var policy: InterpolatePolicy = Linear // default policy
  private var xs: List[T] = _

  private def getPointPerDim: List[Int] = {
    val ret = List.newBuilder[Int]
    for(d <- 0 until dim) {
      ret += paramValues.keys.view.map(_(d)).toVector.distinct.length
    }
    ret.result()
  }

  def createConfig = InterpolateConfig(dataType, dim)

  def input(x: T*): Interpolate[T] = {
    this.xs = x.toList
    this
  }
  def input(x: List[T]): Interpolate[T] = {this.xs = x; this}
  def use(policy: InterpolatePolicy): Interpolate[T] = { this.policy = policy; this }

  /**
   * Function that create the corresponding IU. Take responsibility of transforming
   * paramValues to the input ports, setup the IU and build it.
   * @return
   */
  def generate() = new ImplicitArea[T] {
    val cfg = createConfig // set the data type and the dimension.
    cfg.setPointPerDim(getPointPerDim) // set the number of point per dimension.
    val iu = policy.implement(cfg) // implement a interpolate unit according to policy, which contains multiple basic units.

    // connect the io with external data
    // connect the input x
    for(i <- iu.xs.indices){iu.xs(i) := xs(i)}
    // connect the input parameters
    for(d <- 0 until dim){
      val param_set = mutable.HashSet[T]()
      paramValues.keys.foreach{params=>
        param_set += params(d)
      }
      param_set.toSeq.reverse.zipWithIndex.foreach({case (param, i) =>
        iu.paramValuePorts.params(d)(i) := param
      })
    }
    // connect the input values
    iu.paramValuePorts.values := Vec(paramValues.values)

    override def implicitValue = iu.y
    override type RefOwnerType = this.type
  }
}

object Interpolate {
  def apply[T <: Data with Num[T]](paramValues: Map[List[T], T]) = new Interpolate[T](paramValues)
  def apply[T <: Data with Num[T]](params: (List[T], T)*): Interpolate[T] = Interpolate[T](params.toMap)
  def apply[T <: Data with Num[T]](paramValues: =>Seq[(List[T], T)]) : Interpolate[T]= {
    val pv = Map.newBuilder[List[T], T]
    paramValues.foreach({case (ps, v) =>
      pv += ps -> v
    })
    val pvmap = pv.result()
    Interpolate(pvmap)
  }
}
