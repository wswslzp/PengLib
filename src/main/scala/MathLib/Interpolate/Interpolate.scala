package MathLib.Interpolate

import spinal.core._

import scala.collection.mutable

trait InterpolatePolicy {
  def ports: Int
  def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]): InterpolateUnit[T]
}
object Nearest extends InterpolatePolicy{
  override def ports = 2
  override def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]) = new NearestInterpolateUnit[T](cfg)
}
object Linear extends InterpolatePolicy {
  override def ports = 2
  override def implement[T <: Data with Num[T]](cfg: InterpolateConfig[T]) = new LinearInterpolateUnit[T](cfg)
}
object Cubic extends InterpolatePolicy {
  override def ports = 3
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
  require(points == (1 << dim), "Insufficient points.")
  private var policy: InterpolatePolicy = Linear // default policy
  private var xs: List[T] = _

  def createConfig = InterpolateConfig(
    dataType, dim
  )

  def input(x: T*): Interpolate[T] = {
    this.xs = x.toList
    this
  }
  def input(x: List[T]): Interpolate[T] = {this.xs = x; this}
  def use(policy: InterpolatePolicy): Interpolate[T] = {
    this.policy = policy
    this
  }

  /**
   * Function that create the corresponding IU. Take responsibility of transforming
   * paramValues to the input ports, setup the IU and build it.
   * @return
   */
  def generate() = new ImplicitArea[T] {
    val cfg = createConfig
    cfg.setPorts(policy.ports)
    val iu = policy.implement(cfg)

    // connect the io with external data
    for(i <- iu.xs.indices){iu.xs(i) := xs(i)}
    for(d <- 0 until dim){
      val param_set = mutable.HashSet[T]()
      paramValues.keys.foreach{params=>
        param_set += params(d)
      }
      param_set.toSeq.reverse.zipWithIndex.foreach({case (param, i) =>
        iu.paramValuePorts.params(d)(i) := param
      })
    }
    iu.paramValuePorts.value := Vec(
      paramValues.values
    )
    override def implicitValue = iu.y
    override type RefOwnerType = this.type
  }
}

object Interpolate {
  def apply[T <: Data with Num[T]](params: (List[T], T)*): Interpolate[T] = {
    new Interpolate[T](params.toMap)
  }

  case class IUTestTop() extends Component {
    val io = new Bundle {
      val x0 = in SInt(32 bit)
      val x1 = in SInt(32 bit)
      val y0 = in SInt(32 bit)
      val y1 = in SInt(32 bit)
      val f0 = in SInt(32 bit)
      val f1 = in SInt(32 bit)
      val f2 = in SInt(32 bit)
      val f3 = in SInt(32 bit)
      val x = in SInt(32 bit)
      val y = in SInt(32 bit)
      val f = out SInt(32 bit)
    }

    //todo: the order of the param-value pairs is tied with the input x, y order.
    //  wrong order leads to a wrong result.
    io.f := Interpolate(
      List(io.x0, io.y0) -> io.f0,
      List(io.x0, io.y1) -> io.f1,
      List(io.x1, io.y0) -> io.f2,
      List(io.x1, io.y1) -> io.f3
    ).input(io.x, io.y).use(Nearest).generate()
  }

  def main(args: Array[String]): Unit = {
    SpinalConfig(
      targetDirectory = "rtl",
      headerWithDate = true,
    ).generateVerilog(IUTestTop())
//    SimConfig
//      .withWave
//      .allOptimisation
//      .workspacePath("tb")
//      .compile(IUTestTop())
//      .doSim("IUTestTop_tb"){dut=>
//        dut.io.x0 #= 0
//        dut.io.x1 #= 10
//        dut.io.y0 #= 0
//        dut.io.y1 #= 10
//        dut.io.f0 #= 0
//        dut.io.f1 #= 100
//        dut.io.f2 #= 50
//        dut.io.f3 #= 0
//        dut.io.x #= 3
//        dut.io.y #= 7
//
//        sleep(10)
//        println(s"out is ${dut.io.f.toLong}")
//      }
  }
}
