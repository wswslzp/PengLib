import MathLib.Number.HComplexConfig
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import scala.language._

object AnalyzerTest {

  def hacker_popcnt(num: Int): Int = {
    val n1 = num - ((num >> 1) & 0x55555555)
    val n2 = (n1 & 0x33333333) + ((n1 >> 2) & 0x33333333)
    val n3 = ((n2 >> 4) + n2) & 0x0f0f0f0f
    val n4 = n3 + (n3 >> 8)
    val n5 = n4 + (n4 >> 16)
    n5 & 0x0000003f
  }

  def main(args: Array[String]): Unit = {
    val test_nums = List(
      "0000111100001111".asBin.toInt,
      "0000111100001110".asBin.toInt,
      "0000110000001111".asBin.toInt,
      "0000011000001110".asBin.toInt
    )

    val results = test_nums.map(hacker_popcnt)
    results.indices.foreach{id=> println(s"${test_nums(id)} has ${results(id)} '1'")}
  }
}
