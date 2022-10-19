package CreditBusTestPackage

import org.scalatest.funsuite._

class CreditBusTest extends AnyFunSuite {
  import spinal.lib.credits._
  import spinal.lib._

  val pipeNum = 8

  test("pipe gen"){
    val rtl = Util.PrintRTL("rtl")(CreditPipe(pipeNum)).printRtl()
    assert(pipeNum == LatencyAnalysis(rtl.toplevel.up.valid, rtl.toplevel.down.valid))
  }

  test("credit to stream half pipe gen") {
    val rtl = Util.PrintRTL("rtl")(CreditStreamHalfPipe(pipeNum)).printRtl()
  }

  test("stream to credit half pipe gen") {
    Util.PrintRTL("rtl")(StreamCreditHalfPipe(pipeNum))
  }

  test("search begin test") {
    Util.PrintRTL("rtl")(CreditStreamPipe(pipeNum))
  }
}
