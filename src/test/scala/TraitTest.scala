import spinal.core._
import spinal.core.sim._
import spinal.lib._

trait TA {
  def printTA() = println("This is TA")
}
class CA extends TA {

}

object TraitTest {
  implicit class TB(ta: TA) {
    def printTB() = println("This is TB")
  }

  def main(args: Array[String]): Unit = {
    val ca = new CA
    ca.printTB()
  }
}
