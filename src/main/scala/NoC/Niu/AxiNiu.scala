package NoC.Niu

import spinal.core._
import spinal.lib._
import bus.amba4.axi._

import NoC._

case class AxiNiu(axi4Config: Axi4Config, flitConfig: FlitConfig) extends Module {
  val io = new Bundle {
    val bus = slave(Axi4(axi4Config))
  }
}
