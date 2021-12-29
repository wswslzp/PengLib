import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.language._

object StubTest {

  case class MA() extends Module{
    val a,b = in UInt(8 bit)
    out(a * b)
//    val c = out(UInt(16 bit))
//    c := a * b
    //    val a, b = in(Vec.fill(2)(UInt(8 bit)))
    //    val prod = out(Vec.tabulate(2)(i => a(i) * b(i)))
    //    val c = prod.map(p=> out(~p))
    //    val c = prod.view.map(p => ~p).map(_.resize(12)).map(out(_)).toVector

    def stub: this.type = this rework {
      this.children.clear()
      this.dslBody.foreachStatements({
        case bt: BaseType if !bt.isDirectionLess =>
        case s => s.removeStatement()
      })
      this.dslBody.foreachStatements({
        case bt: BaseType if bt.isOutputOrInOut =>
          bt := bt.getZero
        case _=>
      })
      this
    }
  }

  def main(args: Array[String]): Unit = {
    import Util._
//    PrintRTL("rtl")(MA().stub)
    PrintRTL("rtl")(MA())
  }

}
