import spinal.core._
import Util._
import spinal.core.internals.Literal

package object EdaAuto {
  // TODO:
  //  1. SDC constraint automate
  //  2. IO file automate

  implicit class SdcClockDomain(cd: ClockDomain) {
    def toSdcClock: SdcClock = SdcClock(
      name = cd.toString(),
      port = cd.clock.getName(),
      period = cd.samplingRate.getValue.toInt / 1e9 // in nanosecond unit
    )
    def toSdcGeneratedClock(source: ClockDomain): SdcGeneratedClock = {
      // Now the clock enable division clock domain not considered
      // as a new clock domain.
      def getClkAbsPath(clk: Bool): String = {
        clk.toString()
        val component = clk.component
        if (clk.isNamed || !clk.hasOnlyOneStatement || !clk.head.source.isInstanceOf[Literal])
          s"${(if (component != null) component.getPath() + "/" else "") + clk.getDisplayName()}"
        else
          clk.head.source.toString
      }
      val factor = cd.clockEnableDivisionRate.getValue.toInt
      val clk = cd.clock
      val clkPath = getClkAbsPath(clk)
      SdcGeneratedClock(
        name = cd.toString(),
        pin = clkPath,
        factor = factor,
        source = source.toSdcClock
      )
    }
  }

}
