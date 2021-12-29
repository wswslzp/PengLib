package NocTest.Axi

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import bus.amba4.axi.sim._
import NoC.Axi._
import spinal.lib.bus.amba4.axi.Axi4Config

object AxiMuxTest {
  def main(args: Array[String]): Unit = {
    val defaultConfig = Axi4Config(32, 32, 4)
    SimConfig.withIVerilog.withWave.allOptimisation.workspacePath("tb")
      .compile(AxiWriteOnlyMux(2, defaultConfig))
      .doSim("test") {dut=>
        import dut._

        val axiDrivers = Vector.fill(4)()
        clockDomain.forkStimulus(2)
        clockDomain.waitSampling()
      }
  }
}
