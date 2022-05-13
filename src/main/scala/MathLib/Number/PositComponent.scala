package MathLib.Number

import scala.language._
import spinal.lib._

/**
 * The bit component of Posit
 * @param es the `es` parameter
 * @param sign the sign number, -1 for negative, 1 for positive and 0 for zero.
 * @param regimeWidth regime bit field width
 * @param regime regime bit field number decoded, as k = rc - 1 when leading bit is 1 else k = -rc, where rc is the running length (identical bit width)
 * @param exponentWidth exponent bit field width
 * @param exponent exponent bit field number decoded, without any offset / bias.
 * @param fractionWidth fraction bit field width
 * @param fraction fraction bit field fixed point number decoded as 1.x, with '1' prepended.
 */
case class PositComponent(es: Int, sign: Int, regimeWidth: Int, regime: Int, exponentWidth: Int, exponent: BigInt, fractionWidth: Int, fraction: Int) {
  import scala.math._

  def runningLen: Int = if(regime < 0) -regime else regime+1
  def isZeroOrInf = (regime == -regimeWidth) && exponentWidth == 0 && fractionWidth == 0
  def isZero = sign == 0 // todo change
  def isInf = isZeroOrInf && sign == -1

  def config = PositConfig(regimeWidth+exponentWidth+fractionWidth+1, es)

  private def log2(x: BigInt): Int = (log(x.doubleValue())/log(2)).toInt

  def encodeRegime: String = {
    val digit = if(regime < 0) "0" else "1"
    val nDigit = if(regime < 0) "1" else "0"
    digit * runningLen + nDigit * (regimeWidth-runningLen)
  }
  def encodeExponent: String = {
    if(exponentWidth == 0) ""
    else log2(exponent).binString(exponentWidth)
  }
  def encodeFraction: String = {
    if(fractionWidth == 0) ""
    else fraction.binString(fractionWidth).drop(1)
  }

  /**
   * encode the Posit component as Posit segment.
   * @return encoded bit segments of Posit
   */
  def encode: PositSegment = {
    val s_sign = if(sign >= 0) "0" else "1"
    if(isZeroOrInf){
      return PositSegment(es = es, sign = s_sign, regime = "0"*regimeWidth, exponent = "", fraction = "")
    }
    val s_regime = encodeRegime
    val s_exponent = encodeExponent
    val s_fraction = encodeFraction
    PositSegment(
      es = es, sign = s_sign, regime = s_regime, exponent = s_exponent, fraction = s_fraction
    )
  }

  def effectiveExp: BigInt = (regime << es) + exponent
  def toDouble: Double = {
    val regScale = pow(config.useed, regime)
    val expScale = exponent.doubleValue()
    val frac = fraction.toDouble / pow(2, fractionWidth)
    sign * regScale * expScale * frac
  }
}

object PositComponent {
  import scala.math._
  def ZERO(width: Int, es: Int) = PositComponent(es, 0, width-1, 0, 0, 0, 0, 0)
  def ZERO(config: PositConfig): PositComponent = ZERO(config.nbits, config.es)
  def INF(width: Int, es: Int) = PositComponent(es, -1, width-1, 0, 0, 0, 0, 0)
  def INF(config: PositConfig): PositComponent = INF(config.nbits, config.es)

  def split(binStr: String, es: Int): PositSegment = PositSegment(binStr, es)
  def decode(segment: PositSegment): PositComponent = {
    val es = segment.es
    if (segment.isInf) {
      return PositComponent(es, -1, segment.regimeWidth, -segment.regimeWidth, 0, 0, 0, 0)
    } else if (segment.isZero) {
      return PositComponent(es, 0, segment.regimeWidth, -segment.regimeWidth, 0, 0, 0, 0)
    }

    val sign = if (segment.sign == "1") -1 else 1
    val regimeW = segment.regimeWidth
    val runningLen = segment.runningLength
    val k = if (segment.regime(0) == '0') -runningLen else runningLen - 1
    val regime = k
    val binExponent = if (segment.exponent.isEmpty) 0 else BigInt(segment.exponent, radix = 2).toInt
    val exponent = BigInt(1) << binExponent
    val exponentW = segment.exponentWidth
    val fraction = BigInt("1" + segment.fraction, radix = 2).toInt
    val fractionW = segment.fractionWidth
    PositComponent(es, sign, regimeW, regime, exponentW, exponent, fractionW, fraction)
  }

  /**
   * Convert the bit string into Posit component
   * @param binStr bit pattern in 01 string
   * @param es exponent width (namely)
   * @return return the posit component
   */
  def apply(binStr: String, es: Int): PositComponent = decode(split(binStr, es))

  /**
   * Convert normal floating point real number into posit representation
   * @param real a real number
   * @return posit component
   */
  def apply(real: Double, bitWidth: Int, es: Int): PositComponent = {
    def roundToZero(d: Double): Double = if(d < 0) ceil(d) else floor(d)
    val config = PositConfig(bitWidth, es)
    if(abs(real) < config.minpos) ZERO(bitWidth, es)
    else {
      val sign = signum(real).toInt
      val x = min(abs(real), config.maxpos)
      val scale = roundToZero(log(x)/log(2))
      val regime = roundToZero(scale / (1 << es))
      val exponent = scale - regime * (1 << es)
      val fraction = x / pow(2, scale) //- 1
      val regimeW = (if(regime >= 0) regime + 2 else -regime + 1).toInt
      val exponentW = min(bitWidth-1 - regimeW, es)
      val fractionW = max(bitWidth-1 - regimeW - exponentW, 0)
      val binExponent = BigInt(1) << (roundToZero(exponent * pow(2, exponentW-es)) * pow(2, es-exponentW)).toInt
      val binFraction = roundToZero(fraction * pow(2, fractionW)).toInt
      PositComponent(
        es = es,
        sign = sign,
        regimeWidth = regimeW,
        regime = regime.toInt,
        exponentWidth = exponentW,
        exponent = binExponent,
        fractionWidth = fractionW,
        fraction = binFraction
      )
    }
  }

  /**
   * Fused multiply-add operation algorithm prototype, y = op1 * op2 + op3, op1~op3 with the same config
   * @param op1 oprand 1
   * @param op2 oprand 2
   * @param op3 oprand 3
   * @param neg true if negate the product
   * @param sub true if product subtract op3
   * @return y
   */
  def fuseMulAdd(op1: PositComponent, op2: PositComponent, op3: PositComponent, neg: Boolean = false, sub: Boolean = false): PositComponent = {
    val retConfig = op1.config
    if(op1.isInf || op2.isInf || op3.isInf) INF(retConfig)
    else if((op1.isZero || op2.isZero) && op3.isZero) ZERO(retConfig)
    else {
      val exp1 = op1.effectiveExp
      val exp2 = op2.effectiveExp
      val exp3 = op3.effectiveExp

      val sign3 = if((op3.sign < 0) ^ neg ^ sub) -1 else 1
      val retSign = if((op1.sign < 0) ^ (op2.sign < 0) ^ neg) -1 else 1
      val retExp = exp1 + exp2
      val retFrac = op1.fraction * op2.fraction
      //todo overflow check
      val isSwap = retExp < exp3 || (retExp == exp3 && retFrac < op3.fraction)
      ???
    }
  }

  def main(args: Array[String]): Unit = {
    val pnum = PositComponent(10.24, 16, 0)
    println(s"useed is ${pnum.config.useed}, minpos is ${pnum.config.minpos}, maxpos is ${pnum.config.maxpos}")
    println(s"pnum is ${pnum.toString}, bin is ${pnum.encode.binString} , value is ${pnum.toDouble}")
  }
}