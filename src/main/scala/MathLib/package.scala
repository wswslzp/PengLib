package object MathLib {
  def twosComplement(n: Int, bits: Int)= ((1 << bits) - n) % (1 << bits)
}
