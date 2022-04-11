import spinal.core._
import spinal.core.sim._
import spinal.lib._
import bus.amba4.axi._

import scala.language.postfixOps
import java.io.File
import scala.collection.mutable
import MemBlackBoxer.PhaseMemBlackBoxer._

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
    val mem = Ram1r1w(MemConfig(32, 32, MemBlackBoxer.Vendor.Huali))
    mem.io.clka := clockDomain.readClockWire
    mem.io.clkb := clockDomain.readClockWire

    mem.io.apa.address := 43
    mem.io.apa.memoryEnable := True
    mem.io.apb.address := 43
    mem.io.apb.memoryEnable := True
    mem.io.dp.writeData := 0xabcd
    mem.io.dp.writeEnable := True

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
    mem.write(in UInt(9 bit), in Bits(32 bit), mask = in Bits(32 bit))
//    mem.addTag(dontBB)
//    out(mem(in UInt(9 bit)))
    out(mem.readSync(in UInt(9 bit)))
  }

  def main(args: Array[String]): Unit = {
    import MemBlackBoxer.Vendor.Huali
    new File("rtl").mkdir()
    val vendor = Huali
    SpinalConfig(
      targetDirectory = "rtl",
      headerWithDate = true
    )
      .addTransformationPhase(new PhaseSramConverter(vendor, policy = blackboxAllWithoutUnusedTag))
      .generateVerilog(MemToy4())
  }

}
