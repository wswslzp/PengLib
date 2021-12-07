import spinal.core._
import spinal.core.sim._
import spinal.lib._
import bus.amba4.axi._

import java.io.File
import scala.collection.mutable

import MemBlackBoxer.PhaseMemBlackBoxer._
import MemBlackBoxer.Vendor._

object MemTest {

  case class MemToy() extends Component {
    val mem = Axi4SharedOnChipRam(32, 512, 5)
    val bus = slave(Axi4Shared(mem.axiConfig))
    mem.io.axi <> bus
  }

  def main(args: Array[String]): Unit = {
    new File("rtl").mkdir()
    val vendor =MemBlackBoxer.MemManager.Huali
    SpinalConfig(
      targetDirectory = "rtl",
      memBlackBoxers = mutable.ArrayBuffer(new PhaseSramConverter(vendor))
    ).generateVerilog(MemToy())
  }

}
