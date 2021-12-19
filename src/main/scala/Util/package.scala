import spinal.core._
import spinal.lib._

package object Util {
  case class CountUpFrom(cond: Bool, length: Int, prefix: String = "") extends Area {
    val counter = Counter(length).setCompositeName(cond, prefix, weak = true)
    val condPeriodMinusOne = RegInit(False).setWhen(cond).clearWhen(counter.willOverflow)
    condPeriodMinusOne.setCompositeName(cond, prefix+"_cond_period_minus_1", weak = true)
    val condPeriod = cond | condPeriodMinusOne
    condPeriod.setCompositeName(cond, prefix+"_cond_period")
    when(condPeriod){
      counter.increment()
    }
  }

  case class CountUpInside(cond: Bool, length: Int, prefix: String = "") extends Area {
    val counter = Counter(length).setCompositeName(cond, name, weak = true)
    when(cond) {
      counter.increment()
    }
    val last = counter.willOverflow
    last.setCompositeName(cond, name+"_last")
  }

  implicit class BoolPimper(signal: Bool) {
    def aftermath(length: Int) = CountUpFrom(signal, length)
    def lasting(length: Int) = CountUpInside(signal, length)
  }

}
