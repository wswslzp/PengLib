import spinal.core._
import spinal.lib._

package object CreditBusTestPackage {
  import spinal.lib.credits._
  case class CreditPipe(n : Int = 8) extends Module {
    val up = slave(CreditBus(Bits(32 bit)))
    val down = master(CreditBus(Bits(32 bit)))

    down < n < up
  }

  case class CreditStreamPipe(n : Int) extends Module {
    val up = slave(Stream(Bits(32 bit)))
    val down = master(Stream(Bits(32 bit)))

    val upCredit = up.toCreditBus(15)
    val downCredit = upCredit.pipe(n)
    down << downCredit.toStream(8)
  }

  case class CreditStreamHalfPipe(n : Int) extends Module {
    val up = slave(CreditBus(Bits(32 bit)))
    val down = master(Stream(Bits(32 bit)))

    down <-< up.toStream(n)
  }

  case class StreamCreditHalfPipe(n : Int) extends Module {
    val up = slave(Stream(Bits(32 bit)))
    val down = master(CreditBus(Bits(32 bit)))
    down < n < up.toCreditBus(2)
  }


}
