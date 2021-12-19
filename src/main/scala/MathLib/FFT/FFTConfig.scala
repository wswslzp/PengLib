package MathLib.FFT

import spinal.core._
import spinal.lib._
import MathLib.Number._

case class FFTConfig
(
  hComplexConfig: HComplexConfig,
  point:Int,
  row:Int,
  row_pipeline: Boolean = false,
  col_pipeline: Boolean = true
) {
  def getTwiddleFactorTable = {
    import scala.math._
    import scala.collection.mutable
    val t = (log(point) / log(2)).toInt
    require(point == pow(2, t).asInstanceOf[Int])

    val twiddle_factor = mutable.ArrayBuffer.fill(point - 1, 2)(0d)
    var L = 2
    var k = 0

    while(L <= point) {
      val theta = 2 * Pi / L
      for (j <- 0 until L/2) {
        twiddle_factor(k)(0) = cos(j*theta)
        twiddle_factor(k)(1) = -sin(j*theta)
        k += 1
      }
      L *= 2
    }

    val table = Vec(HComplex(hComplexConfig), point-1)
    table.zipWithIndex foreach { case(dat, idx) =>
      dat.real := twiddle_factor(idx)(0)
      dat.imag := twiddle_factor(idx)(1)
    }

    table
  }
}

object FFTConfig {
  object FFTMode extends SpinalEnum(binarySequential) {
    val col_parallel, row_serial = newElement()
  }
  object ConjMode extends SpinalEnum(binarySequential) {
    val former_conj, back_conj, no_conj = newElement()
  }
}