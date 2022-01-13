package MemBlackBoxer.MemManager

import spinal.core._

trait MemBlackBox extends BlackBox {
//  setBlackBoxName(mem_cfg.name)

  def connectPort(): MemBlackBox
  protected var name: String

  addPrePopTask(() => setBlackBoxName(name))
}

abstract class SinglePortBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "_1p1rw"
}

abstract class DualPortBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "_2p2rw"
}

abstract class TwoPortBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "_2p1r1w"
}

abstract class RomBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "_rom"
}
