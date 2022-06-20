package NoC.Niu
import scala.language.postfixOps

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

import NoC._



/**
 * Packer for every axi channel
 * @param config
 * @param axi4Config
 * @tparam T
 */
case class Axi4Packer[T<:Data](config: PackerConfig[T], axi4Config: Axi4Config) extends Module {
  private val PackerConfig(packetType, flitConfig, dt) = config
  val io = new Bundle {
    val axi = master(Axi4(axi4Config))
    val flit = slave(Stream(Flit(flitConfig, dt())))
  }
  noIoPrefix()

  // id remapper
  //
}
