package MathLib.Number

import spinal.lib._

import scala.language._

case class PositSegment(es: Int, sign: String, regime: String, exponent: String, fraction: String) {
  def bitWidth = sign.length + regime.length + exponent.length + fraction.length

  def regimeWidth = regime.length

  def regimeRange = 1 until regimeWidth

  def runningLength = if (regime.distinct.length == 1) regimeWidth else regimeWidth - 1

  def exponentWidth = exponent.length

  def hasExponent = exponentWidth != 0

  def exponentRange = regimeWidth until exponentWidth

  def fractionWidth = fraction.length

  def hasFraction = fractionWidth != 0

  def fractionRange = exponentWidth until bitWidth

  def isZeroOfInf = regime.forall(_ == '0') && exponentWidth == 0 && fractionWidth == 0

  def isZero = isZeroOfInf && sign == "0"

  def isInf = isZeroOfInf && sign == "1"

  def decode: PositComponent = PositComponent.decode(this)

  def binString: String = {
    if(isInf) sign + "0" * (bitWidth-1)
    else if(isZero) sign * bitWidth
    else if(sign == "0") sign + regime + exponent + fraction
    else {
      val compl = "0" + regime + exponent + fraction
//      println(s"regime: $regime, exp: $exponent, frac: $fraction, comp: $compl")
      val complPattern = compl.asBin.toInt
      sign + MathLib.twosComplement(complPattern, bitWidth-1).binString(bitWidth-1)
    }
  }

  def config = PositConfig(bitWidth, es)
}

object PositSegment {
  def apply(binStr: String, es: Int): PositSegment = {
    require(binStr.forall(c => c == '0' || c == '1'), "binary string pattern should only contain 0 or 1.")
    if (binStr.forall(_ == '0')) {
      return PositSegment(es, "0", "0"*(binStr.length-1), "", "") // todo check
    } else if (binStr.head == '1' && binStr.drop(1).forall(_ == '0')) {
      return PositSegment(es, "1", "0"*(binStr.length-1), "", "")
    }

    val sign = binStr(0).toString
    val twoComplementBinStr = if (sign == "0") binStr else {
      val intPattern = binStr.asBin.toInt
      val twoComplement = MathLib.twosComplement(intPattern, binStr.length)
      twoComplement.binString(binStr.length)
    }

    val leadingBitInRegime = twoComplementBinStr(1)
    var runningLen = 1
    while ((runningLen < twoComplementBinStr.length) && (twoComplementBinStr(runningLen) == leadingBitInRegime)) {
      runningLen += 1
    }
    runningLen = if (runningLen == twoComplementBinStr.length) runningLen - 1 else runningLen

    val regimeEndBitPos = runningLen // from MSB[0] to LSB[str.length-1]
    val regime = (1 to regimeEndBitPos).map(twoComplementBinStr(_)).mkString
    val exponentEndBitPos = if (regimeEndBitPos + es < twoComplementBinStr.length) regimeEndBitPos + es else twoComplementBinStr.length - 1
    val hasFractionBit = exponentEndBitPos != (twoComplementBinStr.length - 1)
    val exponent = (regimeEndBitPos + 1 to exponentEndBitPos).map(twoComplementBinStr(_)).mkString
    val fraction = if (hasFractionBit) (exponentEndBitPos + 1 until twoComplementBinStr.length).map(twoComplementBinStr(_)).mkString else ""
    PositSegment(es, sign, regime, exponent, fraction)
  }
}