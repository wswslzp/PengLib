package MathLib.Function.WallaceTree

import scala.language._
import spinal.core._

/**
 * The compressor (4,2)
 *
 * Actually, with
 */
case class Compress42() extends Module {
  val io = new Bundle {
    val x = in (Vec.fill(4)(Bool()))
    val cin = in Bool()
    val y = out(Vec.fill(2)(Bool()))
    val cout = out Bool()
  }
  noIoPrefix()


}
