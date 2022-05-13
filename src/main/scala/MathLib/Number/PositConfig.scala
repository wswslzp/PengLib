package MathLib.Number

import spinal.core._
import spinal.lib._
import scala.language._
import scala.math._

case class PositConfig(nbits: Int, es: Int) {
  def numberPattern = BigInt(1) << nbits
  def useed = (BigInt(1) << (1 << es)).doubleValue()
  def minpos = pow(useed, 2-nbits)
  def inf = BigInt(1) << (nbits - 1)
  def maxpos = pow(useed, nbits-2)
  def zero = 0
}

object PositConfig {
  def main(args: Array[String]): Unit = {
    val width = 8
    val es = 0
    val total = 1 << width
    var eq = 0
    for(num <- 0 until (1 << width)) {
      val pattern = num.binString(width)
      val positNum = PositSegment(pattern, es)
      val decoded = PositComponent.decode(positNum)
      val encoded = decoded.encode
      println(s"value is ${decoded.toDouble}")
      if(positNum == encoded) eq += 1
      else {
        println(Console.RED + "WRONG: " + Console.RESET)
        println(s"number[$num]: $pattern")
        println(s"split as $positNum")
        println(s"decode as $decoded")
        println(s"re-encode as $encoded")
        println("="*100)
      }
    }
    println(s"correct: $eq / $total")
  }
}
