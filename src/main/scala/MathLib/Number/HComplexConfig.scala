package MathLib.Number

import spinal.core._

case class HComplexConfig(intw: Int,
                          fracw: Int,
                          useGauss: Boolean = true, // Use Gauss multiplication will decrease area but increase computation time.
                          real_high: Boolean = false,
                          fpga_impl: Boolean = true) {

  import scala.math._

  def getDataWidth: Int = intw + fracw

  def getComplexWidth: Int = (intw + fracw) * 2

  def getDataBitRange: Range = getComplexWidth - 1 downto 0

  def getRealBitRange: Range = if (real_high) {
    getComplexWidth - 1 downto getDataWidth
  } else {
    getDataWidth - 1 downto 0
  }

  def getImagBitRange: Range = if (real_high) {
    getDataWidth - 1 downto 0
  } else {
    getComplexWidth - 1 downto getDataWidth
  }

  def getDataResolution: Double = pow(2, -fracw)

  def getDataMaxValue: Double = pow(2, intw - 1) - pow(2, -fracw)

  def getDataMinValue: Double = pow(2, intw)

  def getDataValueRange: Vector[Double] = (1 to getDataWidth).map(_.toDouble).map(_ / pow(2, intw - 1)).toVector

  def sq: QFormat = SQ(intw + fracw, fracw)

  def minExp: ExpNumber = -fracw exp

  def maxExp: ExpNumber = (intw - 1) exp

  def +(that: HComplexConfig): HComplexConfig = {
    require(this.useGauss == that.useGauss && this.real_high == that.real_high)
    HComplexConfig(
      intw = Math.max(this.intw, that.intw),
      fracw = Math.max(this.fracw, that.fracw),
      useGauss, real_high, fpga_impl
    )
  }

  def +(that: QFormat): HComplexConfig = {
    HComplexConfig(
      intw = Math.max(this.intw, that.width - that.fraction),
      fracw = Math.max(this.fracw, that.fraction),
      useGauss, real_high, fpga_impl
    )
  }

  def <<(rank: Int): HComplexConfig = HComplexConfig(
    intw = this.intw + rank,
    fracw = this.fracw - rank,
    useGauss, real_high, fpga_impl
  )

  def >>(rank: Int): HComplexConfig = this.<<(-rank)

  def <(rank: Int): HComplexConfig = HComplexConfig(
    intw = this.intw + rank,
    fracw, useGauss, real_high, fpga_impl
  )

  def >(rank: Int): HComplexConfig = HComplexConfig(
    intw, this.fracw + rank, useGauss, real_high, fpga_impl
  )

  def <>(rank: Int): HComplexConfig = HComplexConfig(
    this.intw + rank, this.fracw + rank, useGauss, real_high, fpga_impl
  )

  def ><(rank: Int): HComplexConfig = HComplexConfig(
    this.intw - rank, this.fracw - rank, useGauss, real_high, fpga_impl
  )

  def *(that: HComplexConfig): HComplexConfig = {
    require(this.useGauss == that.useGauss && this.real_high == that.real_high)
    HComplexConfig(
      intw = this.intw + that.intw,
      fracw = this.fracw + that.fracw,
      useGauss, real_high, fpga_impl
    )
  }

  def *(that: QFormat): HComplexConfig = {
    HComplexConfig(
      intw = this.intw + that.width - that.fraction,
      fracw = this.fracw + that.fraction,
      useGauss, real_high, fpga_impl
    )
  }

  def /(that: Int): HComplexConfig = {
    HComplexConfig(
      intw = this.intw / that,
      fracw = this.fracw / that,
      useGauss, real_high, fpga_impl
    )
  }

  def ==(that: HComplexConfig): Boolean = {
    (that.intw == this.intw) && (that.fracw == this.fracw) && (that.useGauss == this.useGauss) && (that.real_high == this.real_high)
  }

  override def toString: String = {
    val s = s"intw: $intw\nfracw: $fracw\nuse_guass: $useGauss\nreal_high: $real_high\n"
    s
  }
}
