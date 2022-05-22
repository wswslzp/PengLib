import scala.language._
import org.scalatest.funsuite.AnyFunSuite
import scala.collection.mutable

class QueueTest extends AnyFunSuite {
  val q = mutable.Queue[Int]()
  q.enqueue(1, 3, 4)

  for(_ <- 0 to 10){
    try {
      println(s"get ${q.dequeue()}")
    } catch {
      case _: java.util.NoSuchElementException=> println("shit")
    }
  }
}
