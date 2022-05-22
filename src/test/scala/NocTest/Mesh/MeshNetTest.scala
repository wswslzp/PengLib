package NocTest.Mesh

import scala.language.postfixOps
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim._
import org.scalatest.funsuite.AnyFunSuite
import NoC._
import Mesh._

import scala.collection.mutable
import scala.util.Random
import com.typesafe._
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level,Logger}

class MeshNetTest extends AnyFunSuite {
  import MeshNet._
  import config._

  Random.setSeed(4)
  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.INFO)
  val logger = scalalogging.Logger("logger")

  val compiled = SimConfig.withWave.compile(MeshNet(config))

  def printFlit(flit: Flit[UInt]): Unit = {
    logger.debug(
      f"""===============================
         |Received transaction ${flit.payload.toLong}:
         |source ID (${flit.attribute.sourceID.x.toLong}, ${flit.attribute.sourceID.y.toLong})
         |target ID (${flit.attribute.targetID.x.toLong}, ${flit.attribute.targetID.y.toLong})
         |===============================
         |""".stripMargin)
  }

  def testbench(dut: MeshNet[UInt]): Unit = {
    dut.clockDomain.forkStimulus(2)

    val loop = 2000
    val tgtQueue = Array.fill(xNum, yNum)(mutable.Queue[Flit[UInt] => Unit]())
    val cmdArray = Array.tabulate(xNum, yNum, routerConfig.portNum)((i, j, k) => {
      val (_, cmdQ) = StreamDriver.queue(dut.io.reqArray(i)(j)(k).input, dut.clockDomain)
      cmdQ
    })
    var error = 0
    onSimEnd {
      println(f"Error cases: $error. Loss rate: ${loop-error}/$loop = ${(1 - error.toFloat/loop) * 100}%%")
    }

    for{
      i <- 0 until xNum
      j <- 0 until yNum
      k <- 0 until routerConfig.portNum
    }{
      StreamReadyRandomizer(dut.io.reqArray(i)(j)(k).output, dut.clockDomain)

      def callback(flit: Flit[UInt]): Unit = {
        if(tgtQueue(i)(j).nonEmpty) {
          logger.debug(s"> router($i, $j) req $k gotcha")
          tgtQueue(i)(j).dequeue()(flit)
        }
        else {
          logger.debug(Console.RED + s"! tgtQueue($i, $j) underflow")
          printFlit(flit)
          logger.debug(Console.RESET)
          error += 1
        }
      }

      StreamMonitor(dut.io.reqArray(i)(j)(k).output, dut.clockDomain)(callback)
    }

    def issue(transID: Int, srcidx: Int = 0, srcidy: Int = 0, tgtidx: Int = 0, tgtidy: Int = 0): Unit = {
      logger.debug(
        f"""-----------------------
           |Issue transaction $transID
           |from ($srcidx, $srcidy)
           |to   ($tgtidx, $tgtidy)
           |-----------------------
           |""".stripMargin)
      val reqChn = Random.nextInt(routerConfig.portNum)

      tgtQueue(tgtidx)(tgtidy) += printFlit
      cmdArray(srcidx)(srcidy)(reqChn) += { flit =>
        flit.attribute.qos.randomize()
        flit.attribute.mode #= 0
        flit.attribute.sourceID.x #= srcidx
        flit.attribute.sourceID.y #= srcidy
        flit.attribute.targetID.x #= tgtidx
        flit.attribute.targetID.y #= tgtidy
        flit.attribute.txnID.randomize()
        flit.payload #= transID
      }
    }

    dut.clockDomain.waitSampling(100)
    for (i <- 0 until loop) {
      val srcidx, srcidy, tgtidx, tgtidy = Random.nextInt(xNum)
      issue(i, srcidx, srcidy, tgtidx, tgtidy)
//      dut.clockDomain.waitSampling(100)
    }
    dut.clockDomain.waitSampling(80000)

    simSuccess()
  }

  test("MeshNet_tb"){
    compiled.doSim("MeshNet_tb"){dut=>
      testbench(dut)
    }
  }

}
