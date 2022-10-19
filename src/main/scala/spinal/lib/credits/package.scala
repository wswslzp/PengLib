package spinal.lib

import spinal.core._
import spinal.core.fiber._
package object credits {
  import CreditBus._
  implicit class StreamCreditPiper[T<:Data](that: Stream[T]) {
    def toCreditBus(maxCreditNum: Int): Handle[CreditBus[T]] = driveFrom(that, maxCreditNum)
    def toCreditBus(maxCreditNum: Int, resetCreditNum: Int): Handle[CreditBus[T]] = driveFrom(that, maxCreditNum, resetCreditNum)
  }
}
