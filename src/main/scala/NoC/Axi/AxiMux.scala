package NoC.Axi

import spinal.core._
import spinal.lib._
import bus.amba4.axi._

import Util._

case class AxiReadOnlyMux(num: Int, config: Axi4Config) extends Module {
  import AxiReadOnlyMux._
  val io = new Bundle {
    val slvs = Vector.fill(num)(slave(Axi4ReadOnly(config)))
    val mst = { // ???
      master(Axi4ReadOnly(config.copy(idWidth = config.idWidth + log2Up(num))))
    }
  }
  noIoPrefix()

  // ID Convert
  val prefixWidth = log2Up(num)
  val idPrependedSlaves = io.slvs.zipWithIndex.map { case (swc, i) =>
    axi4ReadOnlyIdConvert(U(i, prefixWidth bit), swc) // here we prepend prefix on the id in read cmd channel
  }

  // arbiter
  val idPrependedReadCmds = idPrependedSlaves.map(_.readCmd)

  val arbitratedReadCmd = StreamArbiterFactory.roundRobin.transactionLock.on(idPrependedReadCmds)
  val selectedReadId = arbitratedReadCmd.id

  // connect to output
  io.mst.readCmd << arbitratedReadCmd

  // route back read response
  val idMsbRange = (config.idWidth + log2Up(num) - 1) downto config.idWidth
  val readRspIdMsb = io.mst.readRsp.id(idMsbRange)

  val deMuxRsp = StreamDemux(io.mst.readRsp, readRspIdMsb, num)
  for(i <- deMuxRsp.indices){
    idPrependedSlaves(i).readRsp << deMuxRsp(i)
  }
}

object AxiReadOnlyMux {
  def axi4ReadOnlyIdConvert(prefix: UInt, slv: Axi4ReadOnly): Axi4ReadOnly = new Composite(slv){
    val slvIdWidth = slv.config.idWidth
    val ret = Axi4ReadOnly(slv.config.copy(idWidth = slvIdWidth + prefix.getWidth))
    ret << slv// the id width may not matched.
    ret.readCmd.id.removeAssignments()
    ret.readCmd.id := prefix @@ slv.readCmd.id
    slv.readRsp.id.removeAssignments()
    slv.readRsp.id := ret.readRsp.id(slvIdWidth-1 downto 0)
  }.ret

  def main(args: Array[String]): Unit = {
    val defaultConfig = Axi4Config(32, 32, 4)
    PrintRTL("rtl")(AxiReadOnlyMux(4, defaultConfig))
  }
}

case class AxiWriteOnlyMux(num: Int, config: Axi4Config, maxPendingNum: Int = 8) extends Module {
  import AxiWriteOnlyMux._
  val io = new Bundle {
    val slvs = Vector.fill(num)(slave(Axi4WriteOnly(config)))
    val mst = { // ???
      master(Axi4WriteOnly(config.copy(idWidth = config.idWidth + log2Up(num))))
    }
  }
  noIoPrefix()

  // id convert
  val prefixWidth = log2Up(num)
  val idPrependSlaves = io.slvs.zipWithIndex.map{
    case (slv, idx) =>
      axi4WriteOnlyIdConvert(U(idx, prefixWidth bit), slv)
  }

  // command arbiter
  val idPrependedWriteCmds = idPrependSlaves.map(_.writeCmd)
  val arbitratedWriteCmd = StreamArbiterFactory.roundRobin.transactionLock.on(idPrependedWriteCmds)
  val selectedWriteId = arbitratedWriteCmd.id

  // Pending ID queue
  val idMsbRange = (config.idWidth + log2Up(num) - 1) downto config.idWidth
  val writeCmdIdMsb = selectedWriteId(idMsbRange)
  val idQueue = StreamFifo(writeCmdIdMsb, maxPendingNum)
  idQueue.io.push.valid := arbitratedWriteCmd.valid
  idQueue.io.push.payload := writeCmdIdMsb
  io.mst.writeCmd << arbitratedWriteCmd.continueWhen(idQueue.io.push.ready) // halt the write transaction when fifo is full.

  // write data
  val writeData = idPrependSlaves.map(_.writeData)
  val selectedWriteData = StreamMux(idQueue.io.pop.payload, writeData)
  io.mst.writeData << selectedWriteData.continueWhen(idQueue.io.pop.valid) // halt when fifo is empty
  idQueue.io.pop.ready := io.mst.writeData.ready & io.mst.writeData.last // when the write transfer ends, the id retires.

  // write response
  val writeRspIdMsb = io.mst.writeRsp.id(idMsbRange)
  val writeRsp = StreamDemux(io.mst.writeRsp, writeRspIdMsb, num)
  for(i <- writeRsp.indices) {
    idPrependSlaves(i).writeRsp << writeRsp(i)
  }
}
object AxiWriteOnlyMux {
  def axi4WriteOnlyIdConvert(prefix: UInt, slv: Axi4WriteOnly): Axi4WriteOnly = new Composite(slv) {
    val slvIdWidth = slv.config.idWidth
    val ret = Axi4WriteOnly(slv.config.copy(idWidth = slvIdWidth + prefix.getWidth))
    ret << slv
    ret.writeCmd.id.removeAssignments()
    ret.writeCmd.id := prefix @@ slv.writeCmd.id
    slv.writeRsp.id.removeAssignments()
    slv.writeRsp.id := ret.writeRsp.id(slvIdWidth-1 downto 0)
  }.ret

  def main(args: Array[String]): Unit = {
    val defaultConfig = Axi4Config(32, 32, 4)
    PrintRTL("rtl")(AxiWriteOnlyMux(4, defaultConfig))
  }
}

case class AxiMux(num: Int, config: Axi4Config, maxPendingNum: Int = 8) extends Module {
  val io = new Bundle {
    val slvs = Vector.fill(num)(slave(Axi4(config)))
    val mst = { // ???
      master(Axi4(config.copy(idWidth = config.idWidth + log2Up(num))))
    }
  }
  noIoPrefix()

  val readOnlyMux = AxiReadOnlyMux(num, config)
  val writeOnlyMux = AxiWriteOnlyMux(num, config, maxPendingNum)

  for(i <- 0 until num) {
    io.slvs(i) >> readOnlyMux.io.slvs(i) // connect read only part
    io.slvs(i) >> writeOnlyMux.io.slvs(i) // connect write only part
  }
  io.mst << readOnlyMux.io.mst
  io.mst << writeOnlyMux.io.mst
}

object AxiMux {
  def main(args: Array[String]): Unit = {
    val defaultConfig = Axi4Config(32, 32, 4)
    PrintRTL("rtl")(AxiMux(4, defaultConfig))
  }
}
