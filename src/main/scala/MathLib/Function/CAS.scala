package MathLib.Function

import spinal.core._

case class CAS() extends Component {
  val io = new Bundle {
    val a = in Bool() // io.a and io.b are addition data input.
    val b = in Bool()
    val ci = in Bool()// io.ci is carry bit input.
    val p = in Bool() // io.p is function selection bit. p=0 when add and p=1 when sub
    val s = out Bool()// io.s is the result
    val co = out Bool()// io.co is the carry bit output.
  }

  io.s := io.a ^ (io.b ^ io.p) ^ io.ci
  io.co := (io.a | io.ci) & (io.b ^ io.p) | (io.a & io.ci)
}

