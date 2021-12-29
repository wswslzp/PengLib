package MathLib.Function

import spinal.core._
import spinal.lib._

/**
 * The module for computing sqrt() using substrate and compare method.
 * `io.din`: 0.a1a2a3...a_{2*width};
 * `io.dout`: 0.q1q2q3...q_{width}
 * @param width represent the output `dout`'s width. Input `din` should have
 *              2*`width`'s bit count.
 */
case class CompareSqrt(width: Int) extends Component {

  /**
   * The Basic building block composing compare sqrt() function
   * @param index The index of process elements, starts from 0 to `width`.
   */
  case class SqrtProcessElement(index: Int) extends Component {
    val io = new Bundle {
      val A_i = in UInt(2 bit)
      val R_i = in SInt(2*index+2 bit)
      val last_q_vec = in Vec(Bool(), index)
      val q_i = out Bool()
      val R_ip1 = out SInt(2*index+4 bit)
    }

    val q_i = io.R_i >= 0
    val R_is = io.R_i @@ io.A_i
    val d = S(0, R_is.getWidth-index-3 bit) @@ io.last_q_vec.asBits.asSInt @@ S(2 -> q_i, 1 -> !q_i, 0 -> true)

    io.q_i := q_i
    io.R_ip1 := q_i ? ( R_is - d ) | ( R_is + d )
  }

  val io = new Bundle {
    val din = in UInt(width * 2 bit)
    val dout = out UInt(width bit)
  }

  val A_list = Array.tabulate(width){idx=>
    io.din(idx*2, 2 bit)
  }.reverse

  val d0 = U"2'b01"
  val R1 = ( A_list.head - d0 ).asSInt
  val Q_list = Vec(Bool(), width)
  val pe_array = Array.tabulate(width){idx=>
    SqrtProcessElement(idx)
  }

  for(idx <- 0 until width){
    val Rin = if(idx == 0) R1 else pe_array(idx-1).io.R_ip1
    val Ain = if(idx == width-1) U(0, 2 bit) else A_list(idx+1)
    Ain.setName(s"Ain_$idx")
    val last_qin = Vec(Q_list.slice(width-idx, width))
    last_qin.setName(s"last_qin$idx")
    pe_array(idx).io.R_i := Rin
    pe_array(idx).io.A_i := Ain
    pe_array(idx).io.last_q_vec := last_qin
    Q_list(width-idx-1) := pe_array(idx).io.q_i
  }

  io.dout := Q_list.asBits.asUInt
}

object CompareSqrt {
  /**
   * The hardware function to compute the sqrt()
   * @param data A unsigned integer input
   * @param supplyBits The zeros bits to be appended to the right of `data` for up-sampling.
   * @return the sqrt() function result.
   */
  def sqrt(data: UInt, supplyBits: Int): UInt = {
    require(supplyBits % 2 == 0)
    val dw = roundUp(widthOf(data), 2).toInt
    val o_dw = dw + supplyBits/2
    val cs = CompareSqrt(o_dw)
    cs.io.din := ( data << supplyBits ).resized // din_width = 2*dw+supplyBits
    cs.io.dout.tag(UQ(o_dw, supplyBits/2)).fixTo(UQ(o_dw, 0)) // dout_width = dw + supplyBits/2
  }

  /**
   * The simplified function to compute the sqrt()
   * @param data A unsigned integer input
   * @return the sqrt() function result
   */
  def sqrt(data: UInt): UInt = {
    val dw = roundUp(widthOf(data), 2).toInt
    sqrt(data, dw)
  }
}
