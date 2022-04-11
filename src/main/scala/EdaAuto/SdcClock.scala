package EdaAuto

import scala.collection.mutable
import scala.language.{implicitConversions, _}

trait HasClockAttribute extends ClockAttribute {
  val clkName : String
  val clockAttributes = mutable.ArrayBuffer[ClockAttribute]()
  def getAttributeCommand(clockName: String): String = {
    clockAttributes.map(_.tclCons(clockName)).mkString("")
  }
  def addClockAttribute(attribute: ClockAttribute*): Unit = clockAttributes ++= attribute
  def tcl: String = tclCons(clkName)
}

case class SdcClock(name: String, port: String, period: Double) extends HasClockAttribute {
  override val clkName = name
  override def tclCons(clockName: String) = {
    s"""
       |###########################################
       |##      Clock domain $clockName
       |###########################################
       |create_clock -period $period -name $name [get_ports $port]
       |${getAttributeCommand(clockName)}
       |""".stripMargin
  }
}

case class SdcGeneratedClock(name: String, pin: String, factor: Int, source: SdcClock) extends HasClockAttribute {
  override val clkName = name
  override def tclCons(clockName: String) = {
    val sourceClkName = source.name
    s"""
       |###########################################
       |##      Sub-clock domain $name
       |###########################################
       |create_generated_clock -name $name -source [get_clocks $sourceClkName] -divide_by $factor [get_pins $pin]
       |${getAttributeCommand(clockName)}
       |""".stripMargin
  }
}

object SdcClock {
  def main(args: Array[String]): Unit = {
    val clk0 = SdcClock("clk0", "toplevel/clk0", 3)
    val tmp = new ClockAttrBuilder().template
    clk0.addClockAttribute(tmp: _*)
    println(clk0.tcl)
  }
}
