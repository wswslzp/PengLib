package MathLib.Function.PrefixComputation

import scala.language.postfixOps
import spinal.core._
import spinal.lib._
import spinal.core.internals.Expression

/**
 * A module that does prefix computation
 * @param dt hardware data type
 * @param binaryOperator binary function
 * @tparam T software data type
 */
class PrefixComputer[T <: Data](dt: HardType[T], binaryOperator: (T, T) => T) extends Module{
  private def getOperatorStage: Int = {
    val a, b = dt()
    val c = binaryOperator(a, b)
    LatencyAnalysis(a.asInstanceOf[Expression], c.asInstanceOf[Expression])
  }
  private val operatorStage = getOperatorStage
  val totalNumOfStage = operatorStage * log2Up(operatorStage)

  require(operatorStage >= 0, "the operator should be at least 0")
  val io = new Bundle {
    val dataIn = in(dt())
    val clear = in Bool()
    val dataOut = out(dt())
  }
  noIoPrefix()

  case class MidStage(stage: Int) extends Area {
    val dataIn, dataOut = dt()
    val cut = Delay(dataIn, 1 << stage)
    dataOut := binaryOperator(cut, dataIn)
  }

  case class EndStage(stage: Int) extends Area {
    val dataIn, dataOut = dt()
    val cut = DelayWithInit(dataOut, (1 << stage) - operatorStage){d=>
      when(io.clear){d := d.getZero}
    }
    dataOut := binaryOperator(cut, dataIn)
  }

  if(operatorStage == 0) new AreaRoot {
    val partialResult = Reg(dt()) init dt().getZero
    val medianResult = binaryOperator(partialResult, io.dataIn)
    partialResult := medianResult
    when(io.clear){
      partialResult := partialResult.getZero
    }
    io.dataOut := partialResult
  } else if(operatorStage == 1) new AreaRoot {
    val partialResult = dt()
    val medianResult = binaryOperator(partialResult, io.dataIn)
    partialResult := medianResult
    when(io.clear){
      partialResult := partialResult.getZero
    }
    io.dataOut := partialResult
  } else new AreaRoot {
    val totalStage = log2Up(operatorStage)
    val midStages = List.tabulate(totalStage)(MidStage)
    val endStage = EndStage(totalStage)
    (midStages.map(_.dataIn) :+ endStage.dataIn) zip (io.dataIn +: midStages.map(_.dataOut)) foreach { case (t, t1) => t := t1 }
    io.dataOut := endStage.dataOut
  }
}

object PrefixComputer {
  def model[T](dataIn: List[T])(op: (T, T)=> T): List[T] = {
    val ret = List.newBuilder[T]
    for(i <- dataIn.indices) {
      if(i == 0) ret += dataIn(i)
      else {
        ret += dataIn.take(i+1).reduce(op)
      }
    }
    ret.result()
  }

  def prefixData[T<:Data](input: T, clear: Bool = False)(op: (T, T)=> T) = new Composite(input){
    val prefixComputer = new PrefixComputer(input,op)
    prefixComputer.io.dataIn := input
    prefixComputer.io.clear := clear
    val ret = prefixComputer.io.dataOut
  }.ret
  def prefixFlow[T<:Data](input: Flow[T], clear: Bool = False)(op: (T, T)=> T) = new Composite(input) {
    val prefixComputer = new PrefixComputer(input.payload,op)
    prefixComputer.io.dataIn := input.payload
    prefixComputer.io.clear := clear
    val retPayload = prefixComputer.io.dataOut
    val retValid = Delay(input.valid, prefixComputer.totalNumOfStage)
    val ret = Flow(retPayload)
    ret.payload := retPayload
    ret.valid := retValid
  }.ret
  def prefixStream[T<:Data](input: Stream[T], clear: Bool = False)(op: (T, T)=> T) = new Composite(input){

  }

  def main(args: Array[String]): Unit = {
//    import Util.PrintRTL
//    val rtl = PrintRTL("rtl1")(accumulator(10))
//    rtl.printPruned()

    val a = List.range(1, 10)
    val b = model(a)(_ + _)
    println(b)
  }
}
