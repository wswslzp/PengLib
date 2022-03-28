package Interpolate

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import scala.language._
import Util._
import spinal.core.internals._

import scala.language.{postfixOps, _}
object DataTag extends SpinalTag

case class Add1(width: Int) extends Module {
  val io = new Bundle {
    val a = in UInt(width bit)
    val a1 = out UInt(width bit)
  }
  io.a1 := io.a + 1
}

case class TagTop() extends Module {
  val io = new Bundle {
    val a = in UInt(32 bit)
    val b = out UInt(64 bit)
    val cond = in Bool()
  }

  val a2 = io.a * io.a
  a2.addTag(DataTag)
  val b2 = RegNext(a2 * 3 + io.a * 2).resize(64)
  val b1 = RegNext(a2 + io.a * 5).resize(64)
  val b3 = io.cond ? b1 | b2
//  b3.addTag(DataTag)

  val add1 = Add1(b3.getWidth)
  val condArea = new Area {
    when(io.cond){
      add1.io.a := ~b3
    } otherwise {
      add1.io.a := b1 ^ b2
    }
  }

  add1.io.a.addTag(DataTag)
  io.b := (add1.io.a1 / 10).resized

  def colorStatement(st: Statement): Unit = st match {
    case _: DataAssignmentStatement=> print(Console.GREEN_B)
    case _: DeclarationStatement=>
    case _: SwitchStatement =>
    case _=> print(Console.GREEN)
  }

  def colorExpression(e: Expression): Unit = e match {
    case d: Data if d.getTags().contains(DataTag) =>
      print(Console.RED)
    case _: AssignmentExpression => print(Console.BLUE_B)
    case _: Literal => print(Console.BLUE)
    case _: Operator => print(Console.YELLOW)
    case _: BinaryMultiplexer => print(Console.YELLOW_B)
    case _: Resize => print(Console.RED_B)
    case _=>
  }

  def addRegOnTagged(st: Statement): Unit = st match {
    case a: AssignmentStatement if a.finalTarget.getTags().contains(DataTag) =>
      println("FIND THE ASSIGNMENT STATEMENT\n" + a.toStringMultiLine())
      a.remapExpressions{
        case bt: BaseType if bt.getTags().contains(DataTag)=>
          Delay(bt, 3)
        case e=> e
      }
    case _=>
  }

  afterElaboration{
    println("="*20+" Print Statement " + "="*20)
    this.dslBody.foreachStatements{s=>
      colorStatement(s)
      addRegOnTagged(s)
      println(s + "::" + s.getClass.getSimpleName)
      s.foreachExpression{e=>
        colorExpression(e)
        println(" "*4 +e.toString() + "::" + e.getClass.getSimpleName)
        e.foreachExpression{se =>
          colorExpression(se)
          println(" "*8 + se.toString() + "::" + se.getClass.getSimpleName)
        }
      }
    }
    println("="*20+"Print Done" + "="*20)
  }
}

object TagTest {
  def main(args: Array[String]): Unit = {
    val config = SpinalConfig(
      targetDirectory = "rtl",
      headerWithDate = true
    )
    config.generateVerilog(TagTop())
  }
}
