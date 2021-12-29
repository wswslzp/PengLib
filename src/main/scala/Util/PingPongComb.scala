package Util

import spinal.core._
import spinal.lib._

import scala.language.implicitConversions

// val pp_div = new Pingpong(3){ DivUnit(8) }
// pp_div.io.a := a
// pp_div.io.b := b
// io.quo := pp_div.io.quo

/**
 * The ping pong wrapper
 * @param wrapped - the module that needs to be parallel.
 *                Note that the module here to be wrapped must be a purely combinational logic,
 *                a single direction data path / network,
 *                which means that the module consumes more than one cycle of the
 *                fast input clock. (A multi-cycle path exists in the module.)
 * @param dc - the parallel level.
 * @tparam T - the module type
 */
class PingPongComb[T<:Component](wrapped: => T, dc: Int) extends Area {

  private val pingPongUnit = wrapped
  private def getOldModuleName: String = {
    pingPongUnit
      .getClass
      .toString
      .replaceAllLiterally("class ", "")
      .split(Array('.', '$'))
      .last
  }
  private def checkIfComb: Boolean = {
    var ret = true
    pingPongUnit.dslBody.walkStatements({
      case d: Data => ret &= d.isComb
      case _=>
    })
    ret
  }

  assert(checkIfComb, "The wrapped module is not pure combinational.")
  val oldModuleName = getOldModuleName
  val newModuleName = "PingPong_" + oldModuleName
  pingPongUnit.setDefinitionName(newModuleName)

  pingPongUnit.rework { // redesign the wrapped unit

    // clean up all the internal logic
    pingPongUnit.cleanUp()

    new AreaRoot { // avoid anonymous signal.
      // Counting for de-mux
      val cnt = CounterFreeRun(dc)

      // fifo to buffer the input
      val unitInputs = pingPongUnit.getAllIo.toList.filter(_.isInput)
      val unitInputsDeMux = unitInputs.map(bt=> Vec(Reg(bt), dc))
      unitInputsDeMux.foreach{dataBuf=>
        dataBuf.foreach {data=>
          data.init(data.getZero)
        }
      }

      // de-mux logic
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
  }

}

object PingPongComb {
  implicit def toImplicit[T <: Component](pingPongProto: PingPongComb[T]): T = pingPongProto.pingPongUnit
  def apply[T<:Component](dc: Int)(wrapped: => T): PingPongComb[T] = new PingPongComb[T](wrapped, dc)
  def apply[T<:Component](wrapped: => T): PingPongComb[T] = new PingPongComb[T](wrapped, dc = 2)
}

