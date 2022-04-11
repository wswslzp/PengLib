import spinal.core._
import spinal.lib._
import EdaAuto._
import spinal.core.ClockDomain.FixedDivisionRate

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

  case class ClockDivider(factor: Int, dutyRate: Double = 0.5) extends Module {
    require(factor > 1)
    val io = new Bundle {
      val newClock = out Bool()
    }
    noIoPrefix()

    val dutyCycle = (factor.toDouble * dutyRate).toInt
    private def getTikTok(num: Int, duty: Int) = new AreaRoot {
      val tiktok = RegInit(False)
      val counter = CounterFreeRun(num)
      val flip = (counter === 0) || (counter === duty)
      when(flip){
        tiktok := !tiktok
      }
    }.tiktok

    if (factor % 2 != 0 && dutyRate == 0.5) new AreaRoot {
      // odd divide with 50% duty rate
      val posTiktok = getTikTok(factor, dutyCycle)
      val negCd = ClockDomain.current.withRevertedClockEdge()
      val negArea = new ClockingArea(negCd){
        val tiktok = getTikTok(factor, dutyCycle)
      }
      io.newClock := posTiktok || negArea.tiktok
    } else new AreaRoot {
      io.newClock := getTikTok(factor, dutyCycle)
    }
  }

  implicit class ClockDomainPimp(cd: ClockDomain) {
    def slowClockBy(factor: Int, name: String): ClockDomain = factor match {
      case f if f == 1 => cd
      case f if f > 1  =>
        val clockDivider = ClockDivider(factor)
        val newClock = clockDivider.io.newClock
        if (name != null) {
          newClock.setName(name)
        }
        val ret = cd.copy(
          clock = clockDivider.io.newClock,
          frequency = FixedFrequency(cd.samplingRate.getValue / factor),
          clockEnableDivisionRate = FixedDivisionRate(factor)
        ).setSyncWith(cd)
        ret
    }
    def slowClockByPll(factor: Int, name: String)(pllClockOut: => Bool) = factor match {
      case f if f == 1 => cd
      case f if f > 1  =>
        val newClock = pllClockOut
        if (name != null) {
          newClock.setName(name)
        }
        val ret = cd.copy(
          clock = newClock,
          frequency = FixedFrequency(cd.samplingRate.getValue / factor),
          clockEnableDivisionRate = FixedDivisionRate(factor)
        ).setSyncWith(cd)
        ret
    }
  }

  implicit class VecPimp[T <: Data](v: Vec[T]) {
    def shuffle(indexMap: Int=> Int): Vec[T] = {
      val ret = cloneOf(v)
      for(i <- v.range){
        ret(indexMap(i)) := v(i)
      }
      ret
    }
  }

}
