package MathLib.Number

import spinal.core._

import scala.language.implicitConversions

abstract class HFix[T <: MultiData](width: Int) {
  val bitVector = Bits(width bit)
  var fraction: Int = 0

  def q: QFormat
  def maxExp: ExpNumber
  def minExp: ExpNumber = -fraction exp

  def fixTo(q: QFormat, roundType: RoundType): T
  def fixTo(q: QFormat): T = fixTo(q, getFixRound())
}

case class HUFix(width: Int) extends HFix[UFix](width) {
  override def maxExp = (width - fraction) exp

  override def q = UQ(width, fraction)
  def uq: QFormat = q

  /**
   * extend the divide operation of original UFix.
   * @param that
   * @return
   */
  def /(that: UFix): UFix = {
    val uni_q = UQ(
      Math.max(width, that.bitCount),
      Math.max(fraction, -that.minExp)
    )
    val bit_vec_t = bitVector.asUInt.tag(uq).fixTo(uni_q)
    val b = that.asBits.asUInt.tag(
      UQ(that.bitCount, -that.minExp)
    ).fixTo(uni_q)
    // append zeros to the right
    val a: UInt = bit_vec_t @@ U(bit_vec_t.getWidth bit, default -> false)
    val quo = a / b //>> bit_vec_t.getWidth // ret has the same q_format as a
    val ret = UFix(uni_q.width - uni_q.fraction exp, -uni_q.fraction exp)
    ret.assignFromBits(
      quo.tag(UQ(2*bit_vec_t.getWidth, bit_vec_t.getWidth)).fixTo(UQ(bit_vec_t.getWidth, uni_q.fraction)).asBits
    )
    ret
  }

  override def fixTo(q: QFormat, roundType: RoundType): UFix = {
    require(!q.signed)
    val ret = UFix((q.width - q.fraction) exp, -q.fraction exp)
    val left_zeros_num = Math.max(
      (q.width - q.fraction) - (this.uq.width - this.uq.fraction), 0
    )
    val right_zeros_num = Math.max(
      q.fraction - this.uq.fraction, 0
    )
    val bit_vec_tmp = (
      B(left_zeros_num bit, default -> false) ## bitVector ## B(right_zeros_num bit, default -> false)
      ).asUInt
    val uq_tmp = UQ(
      this.uq.width + left_zeros_num + right_zeros_num,
      this.uq.fraction + right_zeros_num
    )
    ret.assignFromBits(bit_vec_tmp.tag(uq_tmp).fixTo(q, roundType).asBits)
    ret
  }
}
object HUFix {
  implicit def toHUFix(that: UInt): HUFix = {
    val ret = HUFix(that.getWidth)
    ret.bitVector := that.asBits
    ret.fraction  = that.Q.fraction
    ret
  }
  implicit def toHUFix(that: UFix): HUFix = {
    val ret = HUFix(that.bitCount)
    ret.bitVector := that.asBits
    ret.fraction  = that.minExp
    ret
  }
}

case class HSFix(width: Int) extends HFix[SFix](width) {
  override def maxExp = (width - fraction - 1) exp
  override def q = SQ(width, fraction)
  def sq = q

  def /(that: SFix) = {
    val uni_q = SQ(
      Math.max(width, that.bitCount),
      Math.max(fraction, -that.minExp)
    )
    val bit_vec_t = bitVector.asSInt.tag(sq).fixTo(uni_q)
    val b = that.asBits.asSInt.tag(
      SQ(that.bitCount, -that.minExp)
    ).fixTo(uni_q)
    // append zeros to the right
    val a: SInt = bit_vec_t @@ S(bit_vec_t.getWidth bit, default -> false)
    val quo = a / that.asBits.asSInt //>> bit_vec_t.getWidth // ret has the same q_format as a
    val ret = SFix(uni_q.width - 1 - uni_q.fraction exp, -uni_q.fraction exp)
    ret.assignFromBits(
      //        quo.tag(SQ(2*bit_vec_t.getWidth, bit_vec_t.getWidth)).fixTo(SQ(bit_vec_t.getWidth, bit_vec_t.getWidth/2)).asBits
      quo.tag(SQ(2*bit_vec_t.getWidth, bit_vec_t.getWidth)).fixTo(SQ(bit_vec_t.getWidth, uni_q.fraction)).asBits
    )
    ret
  }

  def %%(that: Int): SFix = {
    val ret = SFix(maxExp, minExp)
    val bit_cut_pos = scala.math.min(that, width)
    val raw = this.bitVector(( bit_cut_pos-1 ) downto 0).resize(width)
    ret.assignFromBits(raw.asBits)
    ret
  }

  def %(that: SFix): SFix = {
    val uni_q = SQ(
      Math.max(width, that.bitCount),
      Math.max(fraction, -that.minExp)
    )
    val bit_vec_t = bitVector.asSInt.tag(sq).fixTo(uni_q)
    val a: SInt = bit_vec_t
    val rem = a % that.asBits.asSInt
    val mid = SFix(uni_q.width - 1 - uni_q.fraction exp, -uni_q.fraction exp)
    val ret = cloneOf(mid)
    mid.assignFromBits(
      rem.asBits
    )
    when(a.msb) {
      ret := mid + that
    } otherwise {
      ret := mid
    }
    ret
  }

  override def fixTo(q: QFormat, roundType: RoundType) = {
    require(q.signed)
    val ret = SFix((q.width - q.fraction - 1) exp, -q.fraction exp)// an extra signed bit in integer
    val left_signs_num = Math.max(
      (q.width - q.fraction) - (this.sq.width - this.sq.fraction), 0
    )
    val right_zeros_num = Math.max(
      q.fraction - this.sq.fraction, 0
    )
    val bit_vec_tmp = (
      B(left_signs_num bit, default -> bitVector.msb) ## bitVector ## B(right_zeros_num bit, default -> false)
      ).asSInt
    val sq_tmp = SQ(
      this.sq.width + left_signs_num + right_zeros_num,
      this.sq.fraction + right_zeros_num
    )
    ret.assignFromBits(
      bit_vec_tmp.tag(sq_tmp).fixTo(q, roundType).asBits
    )
    ret
  }
}
object HSFix {
  implicit def toHSFix(that: SInt): HSFix = {
    val ret = new HSFix(that.getBitsWidth)
    ret.bitVector := that.asBits
    ret.fraction = that.Q.fraction
    ret
  }

  implicit def toHSFix(that: SFix): HSFix = {
    val ret = new HSFix(that.bitCount)
    ret.bitVector := that.raw.asBits
    ret.fraction = -that.minExp
    ret
  }
}
