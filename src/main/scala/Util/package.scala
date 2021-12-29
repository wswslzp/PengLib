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

  implicit class ModulePimp(module: Module) {
    def stub(): Unit = module.rework {
      module.children.clear()
      module.dslBody.foreachStatements({
        case d: Data if !d.isDirectionLess =>
        case s => s.removeStatement()
      })
      module.dslBody.foreachStatements({
        case d: Data if d.isOutputOrInOut =>
          d := d.getZero
        case _=>
      })
    }

    /**
     * Carefully use
     */
    def cleanUp(): Unit = module.rework {
      module.children.clear()
      module.dslBody.foreachStatements({
        case d: Data if !d.isDirectionLess =>
        case s => s.removeStatement()
      })
    }
  }

  object PrintRTL {
    def apply[T <: Component](path: String)(module: =>T): SpinalReport[T] = {
      val config = SpinalConfig(
        targetDirectory = path
      )
      config.generateVerilog(module)
    }
  }
}
