package MathLib.Interpolate

import spinal.core._

/**
 * Define the multi-dimension grid parameter points.
 * @param cfg
 * @tparam T
 */
class GridPoint[T<:Data with Num[T]](cfg: InterpolateConfig[T]) extends Bundle {
  val params = Vec.tabulate(cfg.totalDim){ d=>
    Vec.fill(cfg.diffPointPerDim(d))(cfg.dataType())
  }
  val values = Vec.fill(cfg.diffPointPerDim.product)(cfg.dataType())
}

/**
 *
 * @param cfg Configuration of the IU
 * @tparam T data type
 */
sealed abstract class InterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends Component {
  var paramValuePorts: GridPoint[T] = _

  var xs: Vec[T] = _
  val y = out(cfg.dataType())

  def getIU(dim: Int): BasicInterpolateUnit[T]
  def getPointPerDim = cfg.diffPointPerDim
  def getDim = cfg.totalDim
  def getDataType = cfg.dataType

  def build(): Unit = {
    // declare ports
    paramValuePorts = in(new GridPoint(cfg))
    paramValuePorts.setName("paramValuePorts")
    xs = in( Vec.fill(cfg.totalDim)(cfg.dataType()) )
    xs.setName("xs")

    // create a new BIU tree
    val tree = new IUTree(this)

    // connect the io ports to the cfg.data vector
    for(d <- 0 until cfg.totalDim){
      tree.inputVector(d) = xs(d) // connect input
      for(p <- 0 until cfg.diffPointPerDim(d)){
        tree.paramVector(d)(p) = paramValuePorts.params(d)(p)
      }
    }
    for(valIndex <- tree.valueVector.indices){
      tree.valueVector(valIndex) = paramValuePorts.values(valIndex)
    }

    // connect the tree's units
    tree.connectUnits()

    y := tree.getOutput
  }

  afterElaboration(build())
}

class NearestInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends InterpolateUnit[T](cfg){
  override def getIU(dim: Int) = new NearestBIU[T](cfg, dim)
  override type RefOwnerType = this.type
}

class LinearInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends InterpolateUnit[T](cfg){
  override def getIU(dim: Int) = new LinearBIU[T](cfg, dim)
  override type RefOwnerType = this.type
}

class CubicInterpolateUnit[T <: Data with Num[T]](cfg: InterpolateConfig[T]) extends InterpolateUnit[T](cfg){
  override def getIU(dim: Int) = new CubicBIU[T](cfg, dim)
  override type RefOwnerType = this.type
}
