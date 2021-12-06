package MemBlackBoxer.Vendor

import spinal.core._
import spinal.lib._
import MemBlackBoxer.MemManager._

class MemBlackBox(mem_cfg: MemConfig) extends BlackBox {
  setBlackBoxName(mem_cfg.name)
}
