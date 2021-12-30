package Interpolate

import MathLib.Interpolate._
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import scala.language._
import Util._

case class NearestTop(num: Int = 4) extends Module {
  val params = Vector.fill(num)(in SInt(16 bit))
  val values = Vector.fill(num)(in SInt(16 bit))
  val io = new Bundle {
    val x = in SInt(16 bit)
    val y = out SInt(16 bit)
  }

  io.y := Interpolate(
    List(params(0)) -> values(0),
    List(params(1)) -> values(1),
    List(params(2)) -> values(2),
    List(params(3)) -> values(3),
  ).input(io.x).use(Nearest).generate()
}

case class NearestTop1(num: Int = 8) extends Module {
  val params = Vector.fill(num)(in SInt(16 bit))
  val values = Vector.fill(num)(in SInt(16 bit))
  val io = new Bundle {
    val x = in SInt(16 bit)
    val y = out SInt(16 bit)
  }

  io.y := Interpolate{
    for(i <- 0 until num) yield {
      List(params(i)) -> values(i)
    }
  }.use(Nearest).input(io.x).generate()
}

/**
 * Square grids, default 3x3
 * @param pointPerDim
 * @param dim
 */
case class NearestTop2(pointPerDim: Int = 3) extends Module {
  val params = Vector.fill(2, pointPerDim)(in SInt(16 bit))
  val values = Vector.fill(pointPerDim * pointPerDim)(in SInt(16 bit))
  val io = new Bundle {
    val x0, x1 = in SInt(16 bit)
    val y = out SInt(16 bit)
  }

  val pointNum = values.length
  io.y := Interpolate {
    for(i <- 0 until pointNum) yield {
      val x = i % pointPerDim
      val y = i / pointPerDim
      List(params(0)(x), params(1)(y)) -> values(i)
    }
  }.use(Linear).input(io.x0, io.x1).generate()

}

case class NearestTop3(xPoint: Int = 2, yPoint: Int = 4) extends Module {
  val xParam = Vector.fill(xPoint)(in SInt(16 bit))
  val yParam = Vector.fill(yPoint)(in SInt(16 bit))
  val values = Vector.fill(xPoint * yPoint)(in SInt(16 bit))
  val io = new Bundle {
    val x, y = in SInt(16 bit)
    val f = out SInt(16 bit)
  }

  val pvlist = for{
      i <- 0 until xPoint
      j <- 0 until yPoint
    } yield {
      List(xParam(i), yParam(j)) -> values(i + j * xPoint)
    }

  io.f := Interpolate(pvlist).use(Nearest).input(io.x, io.y).generate()
}

object NearestTest {

  def main(args: Array[String]): Unit = {
    PrintRTL("rtl")(NearestTop1())
  }
}
