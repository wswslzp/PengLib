import spinal.core._
import spinal.lib._
import spinal.core.sim._

import scala.util.Random
import scala.collection.mutable

package object NocTest {

  class StreamDriver1[T <: Data](stream : Stream[T], clockDomain: ClockDomain, var driver : (T) => Boolean){
    var transactionDelay : () => Int = () => {
      val x = Random.nextDouble()
      (x*x*10).toInt
    }


    //The  following commented threaded code is the equivalent to the following uncommented thread-less code (nearly)
    //  fork{
    //    stream.valid #= false
    //    while(true) {
    //      clockDomain.waitSampling(transactionDelay())
    //      clockDomain.waitSamplingWhere(driver(stream.payload))
    //      stream.valid #= true
    //      clockDomain.waitSamplingWhere(stream.ready.toBoolean)
    //      stream.valid #= false
    //      stream.payload.randomize()
    //    }
    //  }

    var state = 0
    var delay = transactionDelay()
    stream.valid #= false
    stream.payload.randomize()

    private var fireCnt = 0
    private var burstLen = 0
    def setLen(len: Int): Unit = burstLen = len

    def fsm(): Unit = {
      state match{
        case 0 => {
          if (delay == 0) {
            state += 1
            fsm()
          } else {
            delay -= 1
          }
        }
        case 1 => {
          if(driver(stream.payload)){
            stream.valid #= true
            state += 1
          }
        }
        case 2 => {
          if(stream.ready.toBoolean){
            println(Console.RED + s"======> fire cnt is $fireCnt, while burst len is $burstLen" + Console.RESET)
            if((burstLen-1) == fireCnt) {
              fireCnt = 0
              stream.valid #= false
              stream.payload.randomize()
              delay = transactionDelay()
              state = 0
            } else {
              fireCnt += 1
              state = 1
            }
            fsm()
          }
        }
      }
    }
    clockDomain.onSamplings(fsm)

    def reset() {
      state = 0
      stream.valid #= false
    }
  }

  object StreamDriver1{
    def apply[T <: Data](stream : Stream[T], clockDomain: ClockDomain)(driver : (T) => Boolean) = new StreamDriver1(stream,clockDomain,driver)

    def queue[T <: Data](stream : Stream[T], clockDomain: ClockDomain) = {
      val cmdQueue = mutable.Queue[(T) => Unit]()
      val driver = StreamDriver1(stream, clockDomain) { p =>
        if(cmdQueue.isEmpty) false else {
          cmdQueue.dequeue().apply(p)
          true
        }
      }
      (driver, cmdQueue)
    }
  }

}
