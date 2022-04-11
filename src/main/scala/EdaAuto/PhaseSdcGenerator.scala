package EdaAuto

import spinal.core._
import spinal.lib._
import scala.language.postfixOps
import internals._
import scala.collection.mutable

class PhaseSdcGenerator(sdcFile: String) extends PhaseMisc {
  override def impl(pc: PhaseContext): Unit = {
    import pc._

    SpinalProgress("Generate SDC constraint.")

    // todo clock tree exploration
    //   auto generate source clock domain definition and
    //   the sink clock domain.
    val cds = mutable.LinkedHashSet[ClockDomain]()
    topLevel walkComponents {m=>
      m.dslBody.walkDeclarations {st=>
        st.foreachClockDomain{cd=>
          cds += cd
        }
      }
    }

    def walkInputs(that: BaseType)(func : BaseNode => Unit): Unit = {
      that.foreachStatements(s => {
        s.foreachDrivingExpression(input => {
          func(input)
        })
        s.walkParentTreeStatementsUntilRootScope(tree => tree.walkDrivingExpressions(input => {
          func(input)
        }))
      })
    }

    def trackClock(clockDomain: ClockDomain): mutable.LinkedHashSet[ClockDomain] = {
      val clockWire = clockDomain.clock
      val ret = mutable.LinkedHashSet[ClockDomain]()
      SpinalInfo(Console.GREEN + s"tracking clock wire $clockWire")
      walkInputs(clockWire) {
        case bt: BaseType =>
          ret += bt.clockDomain
          SpinalInfo(Console.GREEN + s"\tthis clock's input is $bt")
          SpinalInfo(Console.GREEN + s"\tmaster clock is ${bt.clockDomain}")
        case _ =>
      }
      ret
    }

    SpinalInfo(s"Clock domains ${cds.toString()}")
    // check if generated clock
    val srcClkSetBuilder = Set.newBuilder[SdcClock]
    val genClkSetBuilder = Set.newBuilder[SdcGeneratedClock]
    cds foreach { cd=>
      cd.samplingRate match {
        case f: ClockDomain.FixedFrequency=>
          val cdDrivingSet = trackClock(cd)
          if (cdDrivingSet.nonEmpty) {
            SpinalInfo(s"Got sub-clock domain ${cd.toString()}")
            SpinalInfo(s"\tclock wire ${cd.clock.toString()}")
            SpinalInfo(s"\tdriving set $cdDrivingSet")
            genClkSetBuilder += cd.toSdcGeneratedClock(cdDrivingSet.head)
          } else {
            SpinalInfo(s"Got clock domain ${cd.toString()}")
            SpinalInfo(s"\tclock wire ${cd.clock.toString()}")
            srcClkSetBuilder += cd.toSdcClock
          }
          SpinalInfo(s"\tfrequency is ${f.getValue.decompose}")
        case _=>
      }
    }
    val srcClkset = srcClkSetBuilder.result()
    val genClkset = genClkSetBuilder.result()

    // add clock attribute
    val attrTemplate = new ClockAttrBuilder().template
    srcClkset.foreach {clk=>
      clk.addClockAttribute(attrTemplate: _*)
    }
    genClkset.foreach {clk=>
      clk.addClockAttribute(attrTemplate: _*)
    }

    // write sdc
    import java.io.PrintWriter
    val writer = new PrintWriter(sdcFile)
    val srcClockCons = srcClkset.map(_.tcl).mkString("")
    val genClockCons = genClkset.map(_.tcl).mkString("")
    writer.write(srcClockCons + genClockCons)
    writer.close()
  }
}

object PhaseSdcGenerator {
  def insertSdcPhase(phases: mutable.ArrayBuffer[Phase]): Unit = {
    phases.append(new PhaseSdcGenerator("Default.sdc"))
  }

  def main(args: Array[String]): Unit = {
    val config = SpinalConfig(
      targetDirectory = "rtl",
      defaultClockDomainFrequency = FixedFrequency(50 MHz)
    )
    config.phasesInserters.append(insertSdcPhase)
  }
}
