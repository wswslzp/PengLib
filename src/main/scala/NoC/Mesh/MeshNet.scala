package NoC.Mesh

import scala.language._
import NoC.{Mesh, _}
import spinal.core._

case class MeshNet[T <: Data](config: MeshConfig[T]) extends Module with RenameFlitPayload {
  import config._
  import MeshType._

  val io = new Bundle {
    val reqArray = Array.fill(xNum, yNum, 4)(RequestChannel(routerConfig))
  }
  val routerArray = Array.tabulate(xNum, yNum){ (i, j)=>
    val ret = Mesh.BufferLessRouter(i, j, routerConfig)
    ret.io.reqIO.zip(io.reqArray(i)(j)).foreach {case (nodeReq, meshReq)=>
      nodeReq <> meshReq
    }
    ret
  }
  for(i <- 0 until xNum) {
    routerArray(i)(0)      shortConnectOn SOUTH // mesh bottom
    routerArray(i)(yNum-1) shortConnectOn NORTH // mesh top
    for(j <- 0 until yNum-1) {
      routerArray(i)(j).assignNext(routerArray(i)(j+1)) (NORTH , SOUTH)
    }
  }
  for(j <- 0 until yNum) {
    routerArray(0)(j)      shortConnectOn WEST // mesh left
    routerArray(xNum-1)(j) shortConnectOn EAST // mesh right
    for(i <- 0 until xNum-1) {
      routerArray(i)(j).assignNext(routerArray(i+1)(j)) (EAST , WEST)
    }
  }
}

object MeshNet {
  val fconfig = FlitConfig()
  val rconfig = RouterConfig(flitConfig = fconfig, dataType = UInt(32 bit), portNum = 4)
  val dim = 4
  val config = MeshConfig(routerConfig = rconfig, xNum = dim, yNum = dim)

  def main(args: Array[String]): Unit = {
    import Util._
    PrintRTL("rtl")(MeshNet(config))
  }
}
