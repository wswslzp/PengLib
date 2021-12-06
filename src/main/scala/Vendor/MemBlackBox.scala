package Vendor

import spinal.core._
import spinal.lib._
import MemManager.MemConfig

class MemBlackBox(mem_cfg: MemConfig) extends BlackBox {
  setBlackBoxName(mem_cfg.name)
}
