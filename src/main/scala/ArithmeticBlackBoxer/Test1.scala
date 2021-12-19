package ArithmeticBlackBoxer

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import internals._

import scala.collection.mutable

object Test1 {

  case class MA() extends Module{
    val a, b = in(Vec.fill(2)(UInt(8 bit)))
    val prod = (Vec.tabulate(2)(i => a(i) * b(i)))
    val c = prod.map(p=> out(~p.resize(12)))
  }

  case class PMA() extends Module {
    val ma = Util.PingPong(2)(MA())
  }

  case class MulUnit(width: Int) extends BlackBox {
    val a, b = in UInt(width bit)
    val prod = out UInt(width*2 bit)
  }

  /**
   * An example of replacing a multiplier operation
   */
  class PhaseMulOp extends PhaseNetlist {
    override def impl(pc: PhaseContext): Unit = {
      import pc._

      var i = 0
      walkDrivingExpression({ expr =>
        println("SHIT GAME!!!" + expr.toStringMultiLine())
//        expr match {
//          case inv: Operator.Bool.Not => inv
//        }
      })

      walkBaseNodes {
        case node: BaseType =>
          println(s"NODE $i: ${node.toString()}"); i+=1
          node.foreachStatements({s=>
            println("="*100)
            println("statement: " + s.toStringMultiLine())
            println("node: "+node)
            println("target: " + s.finalTarget.toStringMultiLine())
            s.foreachExpression({
              expr => println("expr:" + expr.toStringMultiLine())
            })
            s.foreachDrivingExpression({
              drv => println("drv: " + drv.toStringMultiLine())
            })
          })
        case mul: Operator.BitVector.Mul=>
          println(s"MUL NODE $i: ${mul.toString()}"); i+=1
          mul.foreachExpression({
            expr => println("mul expr:" + expr.toStringMultiLine())
          })
        case _=>
      }

      walkStatements {
        case s: AssignmentStatement =>
          s.foreachDrivingExpression({
            case expr: Operator.BitVector.Mul=>
              s.component.rework {
                val leftNumber = expr.left.asInstanceOf[UInt]
                val rightNumber = expr.right.asInstanceOf[UInt]
                val productNumber = s.finalTarget.asInstanceOf[UInt]
                val mulUnit = MulUnit(leftNumber.getWidth)
                mulUnit.a.assignFrom(leftNumber)
                mulUnit.b.assignFrom(rightNumber)
                productNumber.removeAssignments()
                productNumber.assignFrom(mulUnit.prod)
              }
            case _=>
          })
        case _=>
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val config = SpinalConfig(targetDirectory = "rtl")
    config.addTransformationPhase(new PhaseMulOp)
    config.generateVerilog(PMA())
  }

}
