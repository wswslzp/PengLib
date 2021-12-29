import spinal.core._
import spinal.lib._
import scala.language._

class ShowMeVal extends spinal.idslplugin.ValCallback {
  private var counter = 0
  override def valCallback[T](ref: T, name: String) = {
    if (name != "counter"){
      counter += 1
      println(s"Get ${counter}th value ${ref}, name is $name")
    }
    ref
  }
}

case class Attr1(value: Int)
case class Attr2(key: String, value: Attr1)

case class Man(age: Int, firstName: String, lastName: String) extends ShowMeVal {
  val fullName = firstName + "_" + lastName
  val a = 3
  val b = 4

  val attr1 = Attr1(a)
  val attr2 = Attr2(lastName, attr1)

  override def toString = {
    s"""
      |We got a man:
      |Age : $age
      |Name: $fullName""".stripMargin
  }
}

object ShowMeVal {
  def main(args: Array[String]): Unit = {
    println(Man(10, "Zhengpeng", "Liao"))
  }
}
