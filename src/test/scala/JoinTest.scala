import spinal.core._
import spinal.core.sim._
import spinal.lib._

case class JoinA() extends Module {
  val sa, sb = slave(Stream(UInt(8 bit)))
  val m = master(Stream(UInt(16 bit)))

  m <-< StreamJoin.arg(sa, sb).translateWith(sa.payload @@ sb.payload)
}

object JoinTest extends App{
  import spinal.lib.sim._
  SimConfig
    .withIVerilog
    .withWave
    .allOptimisation
    .workspacePath("tb")
    .compile(JoinA())
    .doSim("test"){dut=>
      import dut._
      clockDomain.forkStimulus(2)
      StreamDriver(sa, clockDomain) { p =>
        p #= 2; true
      }
      StreamDriver(sb, clockDomain){p=>
        p #= 3; true
      }
      StreamMonitor(m, clockDomain){p=>
        println(s"out is ${p.toBigInt.toString(16)}")
      }
      StreamReadyRandomizer(m, clockDomain)
      clockDomain.waitSampling(1000)
      simSuccess()
    }
}
