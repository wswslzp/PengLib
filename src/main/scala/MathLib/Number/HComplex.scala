package MathLib.Number

import spinal.core._

case class HComplex(config: HComplexConfig) extends Bundle with Num[HComplex] {
  import HSFix._
  def dataWidth = config.intw + config.fracw
  val real, imag = SFix((config.intw-1) exp, -config.fracw exp)

  override def Q = SQ(config.getDataWidth, config.fracw)

  override def asBits: Bits = {
    if(config.real_high) real.asBits ## imag.asBits
    else imag.asBits ## real.asBits
  }

  def conj: HComplex = {
    val ret = HComplex(config)
    ret.real := this.real
    ret.imag.assignFromBits(( -this.imag.asBits.asSInt ).asBits)
    ret
  }

  def abs: UFix = {
    val ret = UFix(config.intw exp, -config.fracw exp)
    val abs_real = (real < 0) ? (-real.asBits.asSInt) | (real.asBits.asSInt)
    val abs_imag = (imag < 0) ? (-imag.asBits.asSInt) | (imag.asBits.asSInt)
    ret.assignFromBits((abs_real.asUInt + abs_imag.asUInt).asBits)
    ret
  }

  override def +(that: HComplex): HComplex = {
    //    require(this.config == that.config)
    val result = HComplex(this.config + that.config)
    result.real := this.real + that.real
    result.imag := this.imag + that.imag
    result
  }

  override def -(that: HComplex): HComplex = {
    //    require(this.config == that.config)
    val result = HComplex(this.config + that.config)
    result.real := this.real - that.real
    result.imag := this.imag - that.imag
    result
  }

  def >>(that: UInt): HComplex = {
    val ret = HComplex(this.config)
    ret.real.assignFromBits(( real.asBits.asSInt |>> that ).asBits)
    ret.imag.assignFromBits(( imag.asBits.asSInt |>> that ).asBits)
    ret
  }

  override def >>(that: Int): HComplex = {
    val ret = HComplex(this.config)
    ret.real.assignFromBits(( real.asBits.asSInt |>> that ).asBits)
    ret.imag.assignFromBits(( imag.asBits.asSInt |>> that ).asBits)
    ret
  }

  def +(that: SInt): HComplex = {
    val ret = HComplex(this.config)
    ret.real := (this.real + that.toSFix).fixTo(Q)
    ret.imag := this.imag
    ret
  }

  def *(that: SFix): HComplex = {
    val real_part = this.real * that
    val imag_part = this.imag * that
    val ret = HComplex(
      intw = real_part.getBitsWidth-real_part.fraction,
      fracw = real_part.fraction
    )
    ret.real := real_part
    ret.imag := imag_part
    ret
  }

  // No pipeline for multiplication
  def doMulOp(that: HComplex): HComplex = {
    val result = HComplex(this.config * that.config)
    if(config.useGauss) {
      val k1 = ( (this.real + this.imag) * that.real )//.fixTo(q_format)
      val k2 = ( (that.imag - that.real) * this.real )//.fixTo(q_format)
      val k3 = ( (that.real + that.imag) * this.imag )//.fixTo(q_format)
      result.real := ( k1 - k3 ).fixTo(result.real.sq)
      result.imag := ( k1 + k2 ).fixTo(result.imag.sq)
    }else{
      result.real := ( this.real * that.real - this.imag * that.imag ).fixTo(result.real.sq)
      result.imag := ( this.real * that.imag + this.imag * that.real ).fixTo(result.imag.sq)
    }
    result
  }

  def /(that: SFix): HComplex = {
    // complex number divide by the real number
    val ret_config = this.config + that.sq
    val ret = HComplex(ret_config)
    val real = this.real//.fixTo(ret_config.sq)
    val imag = this.imag//.fixTo(ret_config.sq)
    val sq_tmp = SQ(
      Math.max(this.config.getDataWidth, that.bitCount),
      Math.max(this.config.fracw, -that.minExp)
    )
    val ia_real = real.fixTo(sq_tmp)
    val ia_imag = imag.fixTo(sq_tmp)
    val ib = that.fixTo(sq_tmp)
    when(that.asBits.asUInt === U(0, that.bitCount bit)) {
      // When the divisor is zero, we set the value to the maximum
      // avoid popping up error signal.
      ret.real.assignFromBits(B(ret.config.getDataWidth bit, default -> true))
      ret.imag.assignFromBits(B(ret.config.getDataWidth bit, default -> true))
    }.otherwise {
      // the quotient is a signed integer
      // discard the remainder.
      ret.real := ia_real / ib
      ret.imag := ia_imag / ib
    }
    ret
  }

  // TODO: When using assignment `:=` provided by spinallib e.g. Flow/Stream/Vec assignment
  //  This assignment method below will be skip and directly use the assignment from SFix.
  def :=(that: HComplex): Unit = {
    if (this.real.sq.fraction < that.real.sq.fraction) {
      //      SpinalInfo(s"this.sq = ${this.real.sq.toString()}")
      //      SpinalInfo(s"that.sq = ${that.real.sq.toString()}, that's name is ${that.getName()}")
      this.real := that.real.fixTo(this.real.sq)
      this.imag := that.imag.fixTo(this.imag.sq)
    } else {
      this.real := that.real.fixTo(this.real.sq)
      this.imag := that.imag.fixTo(this.imag.sq)
      //      this.real := that.real
      //      this.imag := that.imag
    }
  }

  def fixTo(cfg: HComplexConfig): HComplex = {
    val ret = HComplex(cfg)
    ret := this
    ret
  }

  def :=(that: Bits): Unit = {
    this := HComplex(this.config, that.resize(this.config.getComplexWidth))
  }

  override def tag(q: QFormat) = {
    this
  }

  override def +^(right: HComplex) = {
    val ret = HComplex(this.config + right.config)
    ret.real.raw := this.real.raw +^ right.real.raw
    ret.imag.raw := this.imag.raw +^ right.imag.raw
    ret
  }

  override def +|(right: HComplex) = {
    val ret = HComplex(this.config + right.config)
    ret.real.raw := this.real.raw +| right.real.raw
    ret.imag.raw := this.imag.raw +| right.imag.raw
    ret
  }

  override def -^(right: HComplex) = {
    val ret = HComplex(this.config + right.config)
    ret.real.raw := this.real.raw -^ right.real.raw
    ret.imag.raw := this.imag.raw -^ right.imag.raw
    ret
  }

  override def -|(right: HComplex) = {
    val ret = HComplex(this.config + right.config)
    ret.real.raw := this.real.raw -| right.real.raw
    ret.imag.raw := this.imag.raw -| right.imag.raw
    ret
  }

  override def *(right: HComplex) = this.doMulOp(right)

  override def /(right: HComplex) = {
    val right_square = right.real * right.real + right.imag * right.imag
    val prod = this * right.conj
    prod / right_square
  }

  override def %(right: HComplex) = ???

  override def <(right: HComplex) = ???

  override def <=(right: HComplex) = ???

  override def >(right: HComplex) = ???

  override def >=(right: HComplex) = ???

  override def <<(shift: Int) = ???

  override def sat(m: Int) = ???

  override def trim(m: Int) = ???

  override def floor(n: Int) = ???

  override def ceil(n: Int, align: Boolean) = ???

  override def floorToZero(n: Int) = ???

  override def ceilToInf(n: Int, align: Boolean) = ???

  override def roundUp(n: Int, align: Boolean) = ???

  override def roundDown(n: Int, align: Boolean) = ???

  override def roundToZero(n: Int, align: Boolean) = ???

  override def roundToInf(n: Int, align: Boolean) = ???

  override def round(n: Int, align: Boolean) = ???

  override type RefOwnerType = this.type
}

object HComplex {
  def apply(intw: Int, fracw: Int): HComplex = {
    HComplex(HComplexConfig(intw, fracw))
  }

  def apply(config: HComplexConfig, bits: Bits): HComplex = {
    val tmp = new HComplex(config)
    val dw = config.intw + config.fracw
    if (config.real_high) {
      tmp.real.assignFromBits(bits(dw*2-1 downto dw))
      tmp.imag.assignFromBits(bits(dw-1 downto 0))
      tmp
    } else {
      tmp.imag.assignFromBits(bits(dw*2-1 downto dw))
      tmp.real.assignFromBits(bits(dw-1 downto 0))
      tmp
    }
  }
}