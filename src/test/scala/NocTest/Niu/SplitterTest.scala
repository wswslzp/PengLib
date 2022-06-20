package NocTest.Niu

import org.scalatest.funsuite._
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

import scala.util.Random
import scala.collection.mutable
import NoC.Niu._
import NocTest.StreamDriver1

class SplitterTest extends AnyFunSuite {
  val maxLen = 32
  val iw = 12
  val ow = 8
  Random.setSeed(572444263)
//  val seeds = List.fill(100)(Random.nextInt())

  val m = SimConfig.withIVerilog.withWave.compile(Splitter(maxLen, iw, ow))

  case class MPacket(iw: Int, ow: Int, burstLen: Int = 1){
    private val alignedSlice = Math.ceil(iw.toDouble * burstLen / ow).toInt
    private val alignedOW = alignedSlice * ow
    val inPackets = List.fill(burstLen)(BigInt(iw, Random))
    private val packet = inPackets.map(_.toBinInts(iw)).reduce(_ ++ _).binIntsToBigInt
    private val packetBins = packet.toBinInts(alignedOW)
    val flits = List.tabulate(alignedSlice){i=>
      packetBins.slice(i*ow, (i+1)*ow).binIntsToBigInt // todo duo le zui hou yi ge
    }

    def length: Int = flits.length
    def print(): Unit = {
      val sep = ", "
      println(
        s"""Packet width: input $iw bit, output $ow bit
           |Total $burstLen Packet: ${packet.hexString()} with burst len $burstLen: [${inPackets.map(_.hexString(iw)).mkString(sep)}]
           |Total $length Flits: ${flits.map(_.hexString(ow)).mkString(sep)},
           |                 [${flits.mkString(sep)}]""".stripMargin)
    }
    def issue(i: Int, drv: StreamDriver1[PayloadWithLength])(b: PayloadWithLength): Unit = {
      b.length #= burstLen
      b.data #= inPackets(i)
      drv.setLen(burstLen)
      println(s"sent $i sub packet ${inPackets(i).hexString()}")
    }
    def genFlitQueue: mutable.Queue[(BigInt, BigInt)] = {
      val ret = mutable.Queue[(BigInt, BigInt)]()
      ret.enqueue(flits.indices.map(i=> BigInt(i)).zip(flits): _*)
      ret
    }
  }

  object Cond {
    var cnt = 0
    def apply(): Boolean = {
      cnt += 1
      cnt > 10
    }
  }

  test("model packet test"){
    for(i <- 1 to 9){
      val tmp = MPacket(12, 8, i)
      tmp.print()
      println(tmp.genFlitQueue)
    }
  }


  test(s"splitter test"){
    val num = 10
    val packets = List.fill(num)(MPacket(iw, ow, Random.nextInt(maxLen/4)+1))
//    val packets = List.fill(num)(MPacket(iw, ow, 7+1))
    val gotPackets = List.fill(num)(mutable.Queue[(BigInt, BigInt)]())

    m.doSim("Splitter_tb"){dut=>
      import dut._
      clockDomain.forkStimulus(2)
      io.input.data #= 0
      io.input.length #= 0
      io.input.valid #= false

      val (drv, cmdQueue) = StreamDriver1.queue(io.input, clockDomain) // todo rewrite a stream driver that support burst

      val randomizer = StreamReadyRandomizer(io.output, clockDomain, Cond.apply)
      val inMonitor = StreamMonitor(io.input, clockDomain){input=>
        println(s"input port received ${input.data.toBigInt.toString(16)} with burst length ${input.length.toBigInt}")
      }

      clockDomain.waitSampling(3)

      var packetCnt = 0
      val outMonitor = StreamMonitor(io.output, clockDomain){output=>
        val flitID = io.outputID.toBigInt
        val flit = output.toBigInt
        //
        gotPackets(packetCnt).enqueue(flitID -> flit)
        if(flitID == packets(packetCnt).length-1) packetCnt += 1
      }

      // issue some transaction
      drv.reset()
      for(i <- packets.indices){
        println(Console.RED + s"issue the $i transaction." + Console.RESET)
        packets(i).print()
//        drv.setLen(packets(i).burstLen) // todo : failed to set burst length.
        for(j <- 0 until packets(i).burstLen){
          cmdQueue += packets(i).issue(j, drv)
        }
//        clockDomain.waitSampling(Random.nextInt(200))
      }
      clockDomain.waitSampling(20000)
    }

    val truePackets = packets.map(_.genFlitQueue)
//    gotPackets.head.dequeue()
//    assert(gotPackets == truePackets)
    for (elem <- gotPackets.zip(truePackets)) {
      assert(elem._1 == elem._2)
    }
  }
}
