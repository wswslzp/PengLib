package MathLib.Function.WallaceTree

import scala.language._
import spinal.core._

/**
 * The compressor (3,2)
 *          x2   x1   x0
 *          |    |    |
 *          ------    |
 *     -----|    |<---
 *     |    |    |
 *     |    ------
 *     |      |
 *     y1     y0
 */
case class Compress32() extends Module {
  val io = new Bundle {
    val x = in(Vec.fill(3)(Bool()))
    val y = out(Vec.fill(2)(Bool()))
  }
  noIoPrefix()

  io.y(1) := io.x(2) & io.x(1) | (io.x(0) & (io.x(2) | io.x(1)))
  io.y(0) := io.x.reduce(_ ^ _)
}
