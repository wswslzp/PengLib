package NoC.Axi

import spinal.core._
import spinal.lib._
import scala.language._
import bus.amba4.axi._

/**
 * The component reducing the ID width. The axi id will be prepended a prefix,
 * due to the axi muxer. For the coherency, we need to shrink the id so as to
 * make all the id transferred on the fabric same width.
 * @param config - the input axi config
 * @param maxIdNum - the maximum unique id number from input
 * @param outIdWidth - the output id width.
 */
case class AxiIdDownSizer(maxIdNum: Int, outIdWidth: Int, config: Axi4Config)

class ReadOnlyIdDownSizer(maxIdNum: Int, outIdWidth: Int, config: Axi4Config) extends Module {
  val io = new Bundle {
    val slv = slave(Axi4ReadOnly(config))
    val mst = master(Axi4ReadOnly(config.copy(idWidth = outIdWidth)))
  }

  io.mst.readCmd << io.slv.readCmd ~~ {pl=>
    val ret = Axi4Ar(pl.config.copy(idWidth = outIdWidth))
    ret := pl.resized
    ret
  }
  io.slv.readRsp << io.mst.readRsp ~~ {pl=>
    val ret = Axi4R(pl.config.copy(idWidth = outIdWidth))
    ret := pl.resized
    ret
  }
  io.mst.readCmd.id.removeAssignments()
  io.slv.readRsp.id.removeAssignments()
}

/**
 * When the maximum unique id number is less than pow(2, `idWidth`),
 * the output id can represent all the coming id.
 * @param outIdWidth - output id width
 * @param config -
 */
case class ReadOnlyIdRemapper(maxIdNum: Int, outIdWidth: Int, config: Axi4Config) extends ReadOnlyIdDownSizer(maxIdNum, outIdWidth, config) {
  case class IdField(maxPendingTransNum: Int) extends Bundle {
    val used = Bool() // indicate that is this field has not been used
    val inputId = UInt(config.idWidth bit) // the input field
    def init(): Unit = {
      used.init(False)
      inputId.init(inputId.getZero)
    }
  }

  val idTable = Vec(Reg(IdField(32)), maxIdNum)
  idTable.foreach(_.init())

  val counters = Vector.fill(maxIdNum)(CounterUpDown(32)) // recording the pending trans number

  val comingReadCmdId = io.slv.readCmd.id

  // coming id compare all the used id field with itself.
  // if hit, then the index is the current output id.
  val compareOH = idTable.map(field => field.used && (field.inputId === comingReadCmdId))
  val isHit = compareOH.reduceBalancedTree(_ | _)
  val hitIndex = OHToUInt(compareOH)

  // if not, then the `nextFreeId` is the output id, and store the input field into the table.
  val unusedFlag = idTable.map(f=> !f.used)
  val nextFreeIdOH = OHMasking.first(unusedFlag)
  val nextFreeId = OHToUInt(nextFreeIdOH) // select the next free output index

  val chosenId = isHit ? hitIndex | nextFreeId
  io.mst.readCmd.id := chosenId.resized

  // store the new id
  when(!isHit && io.slv.readCmd.fire) {
    idTable(nextFreeId).used.set()
    idTable(nextFreeId).inputId := comingReadCmdId
  }

  // recover the input id from output id
  val comingReadRspId = io.mst.readRsp.id // todo error check. assertion need.
  val chosenField = idTable(comingReadRspId.resized)
  val recoveredId = chosenField.inputId
  io.slv.readRsp.id := recoveredId

  // error process,
  // when the coming resp id is not found in the table
  // slave read resp (on the master port) need to stall
  val error = !chosenField.used
  io.mst.readRsp.ready.removeAssignments()
  io.mst.readRsp.ready := io.slv.readRsp.ready && error

  // counter select
  val incrCounterOH = isHit ? compareOH.asBits() | nextFreeIdOH.asBits
  for(i <- counters.indices){
    // here to increment or decrement the counters
    val willIncr = io.mst.readCmd.fire && incrCounterOH(i)
    willIncr.setName(s"willIncrCnt$i")
    val willDecr = io.slv.readRsp.fire && (comingReadRspId === i)
    willDecr.setName(s"willDecrCnt$i")
    when(willIncr && (! willDecr)){
      counters(i).increment()
    } elsewhen(willDecr){
      counters(i).decrement()
    }

    // here to clear the used flag
    val willComplete = (counters(i).value === 1) && counters(i).decrementIt
    willComplete.setName(s"willCompleteCnt$i")
    idTable(i).used.clearWhen(willComplete)
  }

  // flow control
  io.slv.readCmd.ready.removeAssignments()
  io.slv.readCmd.ready := io.mst.readCmd.ready && (! counters.map(_.willOverflowIfInc).read(chosenId))

}

object ReadOnlyIdRemapper {
  def main(args: Array[String]): Unit = {
    val config = Axi4Config(32, 32, 8)
    import Util._
    PrintRTL("rtl")(ReadOnlyIdRemapper(8, 4, config))
  }
}

case class ReadOnlyIdSerializer(maxIdNum: Int, outIdWidth: Int, config: Axi4Config) extends ReadOnlyIdDownSizer(maxIdNum, outIdWidth, config) {

}

class WriteOnlyIdDownSizer(outIdWidth: Int, config: Axi4Config) extends Module {
  val io = new Bundle {
    val slv = slave(Axi4WriteOnly(config))
    val mst = master(Axi4WriteOnly(config.copy(idWidth = outIdWidth)))
  }
}