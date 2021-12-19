import spinal.core._
import spinal.core.sim._
import spinal.lib._
import bus.amba4.axi._
import bus.amba4.axi.sim._

import scala.collection.mutable

case class AxiPeripheral(busConfig: Axi4Config) extends Module {
  val bus = slave(Axi4(busConfig))
  val ctrl = new Axi4SlaveFactory(bus)
  val ioName = in(Bits(busConfig.dataWidth bit))

  val theName = ctrl.createReadOnly(Bits(busConfig.dataWidth bit), 0xabcd, 0)
  val theAge = ctrl.createReadAndWrite(Bits(busConfig.dataWidth bit), 0xabe0)

  theName := ioName

  out(theAge)
  out(theName)

  theName.simPublic()
  theAge.simPublic()
}

object AxiPeripheral {
  val busConfig = Axi4Config(32, 32, 4,
    useRegion = false, useQos = false, useProt = false, useCache = false, useLock = false)

  def main(args: Array[String]): Unit = {
    SpinalConfig(
      targetDirectory = "rtl"
    ).generateVerilog(AxiPeripheral(busConfig))
  }
}

object Axi4Test{
  val busConfig = Axi4Config(32, 32, 4,
    useRegion = false, useQos = false, useProt = false, useCache = false, useLock = false)

  class Axi4Driver(bus: Axi4) {
    private val clock = bus.component.clockDomain
    private val config = bus.config

    def init(): Unit = {
      bus.aw.valid #= false
      bus.w.valid #=false
      bus.b.ready #=false
      bus.ar.valid #= false
      bus.r.ready #=false
    }

    def driveData(data: Vector[Int], address: Int, cycleBetween: Int = 1): Unit = {
      bus.writeRsp.ready #= true
      fork{
        bus.writeCmd.valid #= true
        bus.writeCmd.addr #= address
        bus.writeCmd.burst #= 0
        bus.writeCmd.size #= log2Up(config.dataWidth/8)
        bus.writeCmd.len #= data.length - 1
        bus.writeCmd.id #= 0
        clock.waitActiveEdgeWhere(bus.writeCmd.ready.toBoolean)
        bus.writeCmd.valid #= false
      }
      clock.waitSampling(cycleBetween)
      fork {
        bus.writeData.last #= false
        bus.writeData.valid #= true
        bus.writeData.strb #= (1 << config.bytePerWord ) - 1
        for(i <- data.indices){
          bus.writeData.data #= data(i)
          bus.writeData.last #= (i == (data.length - 1))
          clock.waitActiveEdgeWhere(bus.writeData.ready.toBoolean)
        }
        bus.writeData.last #= false
        bus.writeData.valid #= false
      }
      fork {
        clock.waitActiveEdgeWhere(bus.writeRsp.valid.toBoolean)
        bus.writeRsp.resp.toInt match {
          case 0 => println("OKAY")
          case 1 => println("EXCLUSIVE OKAY")
          case 2 => println("SLAVE ERROR")
          case 3 => println("DECODE ERROR")
        }
        bus.writeRsp.ready #= false
      }
    }

    def driveOneData(data: Int, address: Int, cyclesCount: Int = 1): Unit = {
      val _data = Vector(data)
      driveData(_data, address, cyclesCount)
    }

    def readData(address: Int, cycleBetween: Int = 1, burstLength: Int = 1): mutable.Queue[Int] = {
      val ret = mutable.Queue[Int]()
      var beatCounts = 0
      bus.readRsp.ready #= true
      fork {
        bus.readCmd.valid #= true
        bus.readCmd.addr #= address
        bus.readCmd.burst #= burstLength
        bus.readCmd.len #= 0
        bus.readCmd.size #= log2Up(config.dataWidth/8)
        bus.readCmd.id #= 1
        clock.waitActiveEdgeWhere(bus.readCmd.ready.toBoolean)
      }
      clock.waitSampling(cycleBetween)
      fork{
        while(beatCounts < burstLength){
          clock.waitActiveEdgeWhere(bus.readRsp.valid.toBoolean)
          ret.enqueue(bus.readRsp.data.toInt)
          beatCounts += 1
        }
      }
      fork {
        clock.waitActiveEdgeWhere(bus.readRsp.last.toBoolean)
        if (beatCounts != burstLength) {
          println(s"Error: The number of beats read is $beatCounts, while the burst length is $burstLength")
        }
      }

      ret
    }
  }

  def main(args: Array[String]): Unit = {
    SimConfig
      .withWave
      .withIVerilog
      .allOptimisation
      .workspacePath("tb")
      .compile(AxiPeripheral(busConfig))
      .doSim("test"){dut=>
        import dut._
        val driver = new Axi4Driver(bus)
        clockDomain.forkStimulus(2)
        driver.init()
        clockDomain.waitSampling()

//        driver.driveData(0xface, 0xabcd)
        ioName #= 0xface
        clockDomain.waitSampling(100)
        val name = driver.readData(0xabcd)
        clockDomain.waitSampling(100)
//        driver.driveOneData(0xb00c, 0xabe0)
        driver.driveData(Vector(0xbc00, 0xb0c0, 0x0b0c, 0xb00c), 0xabe0)
        clockDomain.waitSampling(100)
        val age = driver.readData(0xabe0)
        clockDomain.waitSampling(100)
        driver.driveOneData(1023, 0x2048)
        clockDomain.waitSampling(100)

        println("name is " + name.map(BigInt(_).toString(16)).mkString(","))
        println("age is " + age.map(BigInt(_).toString(16)).mkString(","))
        simSuccess()
      }
  }
}
