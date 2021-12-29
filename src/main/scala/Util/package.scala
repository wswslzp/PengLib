import spinal.core._
import spinal.lib._

package object Util {
  case class CountUpFrom(cond: Bool, length: Int) extends Composite(cond) {
    val counter = Counter(length)
    val condPeriodMinusOne = RegInit(False).setWhen(cond).clearWhen(counter.willOverflow)
    val condPeriod = cond | condPeriodMinusOne
    when(condPeriod){
      counter.increment()
    }
  }

  case class CountUpInside(cond: Bool, length: Int) extends Composite(cond) {
    val counter = Counter(length)
    when(cond) {
      counter.increment()
    }
    val last = counter.willOverflow
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
     * Carefully use, the outputs have no connections.
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
        targetDirectory = path,
        headerWithDate = true
      )
      config.generateVerilog(module)
    }
  }
}
