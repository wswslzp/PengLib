package NoC.Mesh

import scala.language._
import spinal.core._
import NoC._

case class MeshConfig[T <: Data](routerConfig: RouterConfig[T], xNum: Int, yNum: Int) {

}
