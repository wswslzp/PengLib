import spinal.core._
import spinal.core.sim._
import spinal.lib._
import bus.amba4.axi._

import java.io.File
import scala.collection.mutable
import MemBlackBoxer.PhaseMemBlackBoxer._
import spinal.core.internals.Operator

object MemTest {

  case class MemToy() extends Component {
    val mem = Axi4SharedOnChipRam(32, 512, 5)
    val bus = slave(Axi4Shared(mem.axiConfig))
    mem.io.axi <> bus
  }

  case class MemToy1() extends Component {
    val mem = Mem(Bits(32 bit), 1024)
    val rd = new Bundle {
      val addr = in UInt(10 bit)
      val en = in Bool()
      val data = out Bits(32 bit)
    }
    val wr = new Bundle {
      val addr = in UInt(10 bit)
      val en = in Bool()
      val data = in Bits(32 bit)
    }
    rd.data := mem.readSync(rd.addr, rd.en)
    mem.write(wr.addr, wr.data, wr.en)
  }

  case class MemToy2() extends Component {
    import MemBlackBoxer.MemManager._
    val mem = Ram1r1w(MemConfig(32, 32, Huali))
    mem.io.clka := clockDomain.readClockWire
    mem.io.clkb := clockDomain.readClockWire

    mem.io.apa.addr := 43
    mem.io.apa.cs := True
    mem.io.apb.addr := 43
    mem.io.apb.cs := True
    mem.io.dp.din := 0xabcd
    mem.io.dp.we := True

  }

  case class MemToy3() extends Module {
    val mem = Mem(Bits(32 bit), 1024)
    val pa = new Bundle {
      val addr = in UInt(10 bit)
      val we = in Bool()
      val wdata = in Bits(32 bit)
      val rdata = out Bits(32 bit)
    }
    val pb = new Bundle {
      val addr = in UInt(10 bit)
      val we = in Bool()
      val wdata = in Bits(32 bit)
      val rdata = out Bits(32 bit)
    }
//    val pc = new Bundle {
//      val addr = in UInt(10 bit)
//      val we = in Bool()
//      val wdata = in Bits(32 bit)
//      val rdata = out Bits(32 bit)
//    }
    pa.rdata := mem.readWriteSync(pa.addr, pa.wdata, True, pa.we)
    pb.rdata := mem.readWriteSync(pb.addr, pb.wdata, True, pb.we)
//    pc.rdata := mem.readWriteSync(pc.addr, pc.wdata, True, pc.we)

  }

  case class MemToy4() extends Module {
    val mem = Mem(Bits(32 bit), 512)
//    mem(in UInt(9 bit)) := in Bits(32 bit)
    mem.write(in UInt(9 bit), in Bits(32 bit))
//    out(mem(in UInt(9 bit)))
    out(mem.readSync(in UInt(9 bit)))
  }

  def main(args: Array[String]): Unit = {
    new File("rtl").mkdir()
    val vendor =MemBlackBoxer.MemManager.Huali
    SpinalConfig(
      targetDirectory = "rtl",
//      memBlackBoxers = mutable.ArrayBuffer(new PhaseSramConverter(vendor))
      memBlackBoxers = mutable.ArrayBuffer(new PhaseMemTopoPrinter)
    ).addStandardMemBlackboxing(blackboxAll).generateVerilog(MemToy4())
  }

}
