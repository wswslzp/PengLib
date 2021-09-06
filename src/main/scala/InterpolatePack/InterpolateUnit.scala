package InterpolatePack

import Tree._
import spinal.core._
import spinal.lib._


/**
 * Define the multi-dimension interpolation port
 * @param cfg config
 * @tparam T data type
 */
case class MultiParamValuePair[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends Bundle {
  import scala.math.pow
  val params = Vec.fill(cfg.dim)(Vec.fill(cfg.ports)(cfg.dataType()))
  val value = Vec.fill(pow(cfg.ports, cfg.dim).toInt)(cfg.dataType())
}

/**
 *
 * @param cfg Configuration of the IU
 * @tparam T data type
 */
sealed abstract class InterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends Component {
  var paramValuePorts: MultiParamValuePair[T] = _

  var xs: Vec[T] = _
  val y = out(cfg.dataType())

  def getIU: BasicInterpolateUnit[T]
  def getPortsNum: Int = cfg.ports
  def getDim = cfg.dim
  def getDataType = cfg.dataType

  def build(): Unit = {
    // declare ports
    paramValuePorts = in( MultiParamValuePair(cfg) )
    paramValuePorts.setWeakName("paramValuePorts")
    xs = in( Vec.fill(cfg.dim)(cfg.dataType()) )
    xs.setWeakName("xs")

    // create a new BIU tree
    val tree = new IUTree(this)

    val ports = cfg.ports
    // connect the io ports to the cfg.data vector
    for(d <- 0 until cfg.dim){
      tree.input_data_vec(d) = xs(d) // connect input
      for(p <- 0 until ports){
        tree.param_data_vec(d)(p) = paramValuePorts.params(d)(p)
      }
    }
    for(cluster <- tree.value_data_vec.indices){
      for(p <- 0 until ports){
        tree.value_data_vec(cluster)(p) = paramValuePorts.value(cluster * ports + p)
      }
    }

    // connect the tree's units
    tree.connectUnits()

    y := tree.getOutput
  }

  afterElaboration(build())
}

class NearestInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends InterpolateUnit[T](cfg){
  override def getIU = new NearestBIU[T](cfg).setWeakName("NearestBIU")
  override type RefOwnerType = this.type
}

class LinearInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends InterpolateUnit[T](cfg){
  override def getIU = new LinearBIU[T](cfg).setWeakName("LinearBIU")
  override type RefOwnerType = this.type
}

class CubicInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends InterpolateUnit[T](cfg){
  override def getIU = new CubicBIU[T](cfg).setWeakName("CubicBIU")
  override type RefOwnerType = this.type
}
