import spinal.core._
import spinal.lib._

// val pp_div = new Pingpong(3){ DivUnit(8) }
// pp_div.io.a := a
// pp_div.io.b := b
// io.quo := pp_div.io.quo
class Pingpong[T<:Component](wrapped: => T, dc: Int) extends Area {

  private val pingPongUnit = wrapped
  val newName = "pingpong_" + pingPongUnit.getClass.toString.replace("class ", "")
  pingPongUnit.setDefinitionName(newName)
  pingPongUnit.addPrePopTask(() => {

    pingPongUnit.dslBody.walkStatements { // remove all the logic and the statement except io
      case d: Data=> if (d.isDirectionLess) {
        d.removeStatement()
      }
      case other=> other.removeStatement()
    }
    pingPongUnit.children.clear() // clear all the children modules.

    pingPongUnit.rework { // redesign the wrapped unit
      // Counting for dispatch
      val cnt = CounterFreeRun(dc)

      // fifo to buffer the input
      val unitInputs = pingPongUnit.getAllIo.toList.filter(_.isInput)
      val unitInputsDeMux = unitInputs.map(bt=> Vec(Reg(cloneOf(bt)), dc))
      unitInputsDeMux.foreach{dataBuf=>
        dataBuf.foreach {data=>
          data.init(data.getZero)
        }
      }

      // dispatch logic
      val sel = (0 until dc).map(cnt.value === _)
      for(inputDataIndex <- unitInputs.indices){
        unitInputsDeMux(inputDataIndex)(cnt.value) := unitInputs(inputDataIndex)
      }

      // slow area
      val buf_area = new SlowArea(dc){
        // replicate the wrapped units
        val wrappedUnits = List.fill(dc)(wrapped)
        for(i <- 0 until dc){
          for(inputDataIndex <- unitInputs.indices){
            val tmp = sel(i) ? unitInputs(inputDataIndex) | unitInputsDeMux(inputDataIndex)(i)
            wrappedUnits(i).getAllIo.toList.filter(_.isInput)(inputDataIndex) := tmp
          }
        }
      }

      // output
      val unitOutputs = pingPongUnit.getAllIo.toList.filter(_.isOutput)
      unitOutputs.foreach {o=>
        o := o.getZero
      }
      val sel_d = sel.map(Delay(_, dc))
      sel_d.foreach(_.init(False))
      for(i <- 0 until dc){
        val tmp = buf_area.wrappedUnits(i).getAllIo.toList.filter(_.isOutput)
        for(outputDataIndex <- unitOutputs.indices){
          when(sel_d(i)) {
            unitOutputs(outputDataIndex) := tmp(outputDataIndex)
          }
        }
      }
    }

  })

}

object Pingpong {
  implicit def toImplicit[T <: Component](pingPongProto: Pingpong[T]): T = pingPongProto.pingPongUnit
  def apply[T<:Component](dc: Int)(wrapped: => T): Pingpong[T] = new Pingpong[T](wrapped, dc)

  def main(args: Array[String]): Unit = {
    SpinalConfig()
  }
}


