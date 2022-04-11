package EdaAuto

import scala.collection.mutable
import scala.language.{implicitConversions, _}

trait ClockAttribute {
  def tclCons(clockName: String): String
}

case class ClockSourceLatency(early: Double, late: Double) extends ClockAttribute {
  override def tclCons(clockName: String) =
    s"""
       |set_clock_latency $early -early -source [get_clocks $clockName]
       |set_clock_latency $late -late -source [get_clocks $clockName]
       |""".stripMargin
}

case class ClockTreeLatency(rise: Double, fall: Double, min: Boolean, max: Boolean) extends ClockAttribute {
  override def tclCons(clockName: String) = if (min){
    s"""
       |set_clock_latency -rise $rise -min [get_clocks $clockName]
       |set_clock_latency -fall $fall -min [get_clocks $clockName]
       |""".stripMargin
  } else if (max) {
    s"""
       |set_clock_latency -rise $rise -max [get_clocks $clockName]
       |set_clock_latency -fall $fall -max [get_clocks $clockName]
       |""".stripMargin
  } else {
    s"""
       |set_clock_latency -rise $rise [get_clocks $clockName]
       |set_clock_latency -fall $fall [get_clocks $clockName]
       |""".stripMargin
  }
}
case class ClockLatency(
                         sourceLatency: ClockSourceLatency,
                         treeLatencies: mutable.ArrayBuffer[ClockTreeLatency],
                       ) extends ClockAttribute {
  override def tclCons(clockName: String) = {
    sourceLatency.tclCons(clockName) + "\n" + treeLatencies.map(_.tclCons(clockName)).mkString("\n")
  }
}

class ClockLatencyBuilder {
  class TreeLatencyBuilder {
    def min(rise: Double, fall: Double) = ClockTreeLatency(rise, fall, min = true, max = false)
    def max(rise: Double, fall: Double) = ClockTreeLatency(rise, fall, min = false, max = true)
  }

  def source(early: Double, late: Double) = ClockSourceLatency(early, late)
  def tree = new TreeLatencyBuilder

  def template = {
    val source = this.source(1, 1)
    val trees = mutable.ArrayBuffer[ClockTreeLatency]()
    trees.append(this.tree.min(1, 1))
    trees.append(this.tree.max(1, 1))
    ClockLatency(source, trees)
  }
}

case class ClockUncertainty(
                             onClock: Double = 0, // only this clock
                             toThisClock: Double = 0,
                             fromThisClock: Double = 0,
                             rise: Boolean = false, fall: Boolean = false,
                             setup: Boolean = false, hold: Boolean = false
                           ) extends ClockAttribute {
  override def tclCons(clockName: String) = {
    val riseFall = if (rise) {"-rise"} else if (fall) {"-fall"} else {""}
    val setupHold = if (setup) {"-setup"} else if (hold) {"-hold"} else {""}
    val toClock = if (toThisClock != 0) {
      s"""
         |set_clock_uncertainty -to [get_clocks $clockName] $riseFall $setupHold $toThisClock
         |""".stripMargin
    } else ""
    val fromClock = if (fromThisClock != 0) {
      s"""
         |set_clock_uncertainty -from [get_clocks $clockName] $riseFall $setupHold $fromThisClock
         |""".stripMargin
    } else ""
    val thisClock =
      s"""
         |set_clock_uncertainty [get_clocks $clockName] $riseFall $setupHold $onClock
         |""".stripMargin
    thisClock + toClock + fromClock
  }
}

class ClockUncertaintyBuilder {
  def template = ClockUncertainty(
    onClock = 1, rise = true, setup = true
  )
}

case class ClockTransition(
                            transitionTime: Double,
                            rise: Boolean = false, fall: Boolean = false,
                            min: Boolean = false, max: Boolean = false
                          ) extends ClockAttribute {
  override def tclCons(clockName: String) = {
    val riseFall = if (rise) {"-rise"} else if (fall) {"-fall"} else {""}
    val maxMin = if (max) {"-max"} else if (min) {"-min"} else {""}
    s"""
       |set_clock_transition $transitionTime $riseFall $maxMin [get_clocks $clockName]
       |""".stripMargin
  }
}

class ClockTransitionBuilder {
  def template = ClockTransition(transitionTime = 1, rise = true, max = true)
}

class ClockAttrBuilder {
  def template = {
    mutable.ArrayBuffer[ClockAttribute](
      new ClockLatencyBuilder().template,
      new ClockUncertaintyBuilder().template,
      new ClockTransitionBuilder().template
    )
  }
}
