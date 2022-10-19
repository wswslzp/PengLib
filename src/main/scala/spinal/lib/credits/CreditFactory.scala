package spinal.lib.credits

import spinal.core._
import spinal.lib._

class CreditFactory extends MSFactory{
  def apply[T <: Data](hardType: HardType[T]) = {
    val ret = new CreditBus[T](hardType)
    postApply(ret)
    ret
  }

  def apply[T <: Data](hardType: => T) : CreditBus[T] = apply(HardType(hardType))
}
