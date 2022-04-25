import MathLib.Number.HComplexConfig
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import scala.language._

object AnalyzerTest {
  import MathLib.FFT._
  import internals._
  import EdaAuto._
  import scala.collection.mutable.ArrayBuffer

  class Phase1 extends PhaseMisc {
    override def impl(pc: PhaseContext): Unit = {
      import pc._
      import ModuleAnalyzer._
      import DataAnalyzer._

//      topLevel.allInputs.foreach(bt=> SpinalInfo(s"get input : $bt"))
//      topLevel.allOutputs.foreach(bt=> SpinalInfo(s"get output : $bt"))
//      topLevel.allClocks.foreach(cd=> SpinalInfo(s"get clock : $cd"))
//      topLevel.allRegisters.foreach(reg=> SpinalInfo(s"get register : $reg"))
//      topLevel.getRegisters({reg=>
//        reg.getName().contains("col")
//      }).foreach(reg=> SpinalInfo(s"get col register : $reg"))

//      def cellNotFixTo(module: Module): Boolean = !module.getDisplayName().contains("fixTo")
//      topLevel.getCells(cellNotFixTo).foreach(cell=> SpinalInfo(s"get sub module : $cell"))
      val top = topLevel.asInstanceOf[FFT2D]
      top.io.line_in.valid.allFanOut.foreach(bt=> SpinalInfo(s"get valid outputs : $bt"))
      top.io.line_out.valid.allFanIn.foreach(bt=> SpinalInfo(s"get valid inputs : $bt"))
    }
  }

  def insertPhase(phases: ArrayBuffer[Phase]): Unit = {
    phases.append(new Phase1)
  }

  def main(args: Array[String]): Unit = {
    val cfg = FFTConfig(HComplexConfig(8, 8), 128, 128)
    SpinalConfig(
      phasesInserters = ArrayBuffer(insertPhase)
    ).generateVerilog(FFT2D(cfg))
  }

}
