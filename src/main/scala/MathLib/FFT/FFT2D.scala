package MathLib.FFT

import spinal.core._
import spinal.lib._

import Util._
import MathLib.Number._

case class FFT2D(cfg: FFTConfig) extends Module {
  import FFT1D.fft

  case class ColAddrArea(use_pip: Boolean) extends Component {
    val row_addr_ov = in Bool()
    val fft_out_vld = in Bool() allowPruning()
    val col_addr = out UInt(log2Up(cfg.point) bit)
    val col_addr_vld = out Bool()

    if(use_pip){
//      val col_addr_area = countUpFrom(row_addr_ov, 0 until cfg.point, "col_addr")
      val col_addr_area = row_addr_ov.aftermath(cfg.point)
      col_addr := col_addr_area.counter.value
      col_addr_vld := col_addr_area.condPeriod
    } else {
      val cnt = Counter(0 until cfg.point, inc = row_addr_ov || fft_out_vld)
      cnt.setCompositeName(this, "col_addr_cnt")
      val cond_period_minus_1 = Reg(Bool()) setWhen row_addr_ov clearWhen cnt.willOverflow
      col_addr_vld := cond_period_minus_1 | row_addr_ov
      col_addr := cnt.value
    }
  }

  val io = new Bundle {
    val line_in = slave(
      Flow(Vec(HComplex(cfg.hComplexConfig), cfg.point))
    )
    val line_out= master(
      Flow(Vec(HComplex(cfg.hComplexConfig), cfg.point))
    )
  }

  // do the row fft
  val fft_row: Flow[Vec[HComplex]] = fft(io.line_in, cfg.row_pipeline)
  fft_row.setName("fft_row")

  // declare a reg array, and push the data into it
  val img_reg_array = Reg(
    Vec(Vec(HComplex(cfg.hComplexConfig), cfg.point), cfg.row)
  )
  val row_addr: Counter = Counter(0 until cfg.row, inc = fft_row.valid)
  when(fft_row.valid) {
    img_reg_array(row_addr) := fft_row.payload
  }

  // TODO: When not using pipeline, fft cannot read data in pipeline,
  //  Data piped in fft should be hold until the result is valid
  val col_addr_area = ColAddrArea(cfg.col_pipeline)
  col_addr_area.row_addr_ov := row_addr.willOverflow

  val col_addr_vld = col_addr_area.col_addr_vld
  val col_addr = RegNext( col_addr_area.col_addr )
  val fft_col_in = Flow(
    Vec(HComplex(cfg.hComplexConfig), cfg.row)
  )
  fft_col_in.payload.zipWithIndex.foreach { case(dat, idx) =>
    dat := img_reg_array(idx)(col_addr)
  }
  fft_col_in.valid := RegNext(col_addr_vld) init False

  val fft_col_out: Flow[Vec[HComplex]] = fft(fft_col_in, cfg.col_pipeline)
  col_addr_area.fft_out_vld := fft_col_out.valid
  fft_col_out.setName("fft_col_out")
  fft_col_out >-> io.line_out
}

object FFT2D {
  /**
   * Do fft2d for an image.
   * Input one row per cycle, while activating `input.valid`
   * @param input input data flow carry with `valid` and `payload` data. The data is
   *              a vector of `HComplex` that represents a row of image.
   * @param row indicate the total row number of the image.
   * @return the output data flow carry with output data `payload` and `valid` signal.
   *         output one row per cycle
   */
  def fft2(input: Flow[Vec[HComplex]], row: Int): Flow[Vec[HComplex]] = {
    val hcfg = input.payload(0).config
    val point = input.payload.length
    val fft_config = FFTConfig(hcfg, point, row)
    val fft2d_core = FFT2D(fft_config)
    fft2d_core.io.line_in <> input
    fft2d_core.io.line_out
  }

  def fft2(input: Flow[Vec[HComplex]], inverse: Bool, row: Int, row_pipeline: Boolean = true, col_pipeline: Boolean = true): Flow[Vec[HComplex]] = {
    val hcfg = input.payload(0).config
    val point = input.payload.length
    val fft_config = FFTConfig(hcfg, point, row, row_pipeline, col_pipeline)
    val fft2d_core = FFT2D(fft_config)
    fft2d_core.io.line_in <> input.translateWith(
      Vec(input.payload.map{dat=>
        inverse ? dat.conj | dat
      })
    )
    fft2d_core.io.line_out.translateWith(
      Vec(fft2d_core.io.line_out.payload.map{dat=>
        inverse ? dat.conj | dat
      })
    )
  }
  /**
   * Do fft2d for an image. Input one pixel per cycle, while activating `input.valid`
   * @param input input data flow carry with `valid` and `payload` data. The data is a
   *              HComplex data that represent a pixel of image.
   * @param row indicate the total row number of the input image
   * @param point indicate the total column number of one row of the input image
   * @return output one row per cycle
   */
  def fft2(input: Flow[HComplex], row: Int, point: Int): Flow[Vec[HComplex]] = {
    // The valid of input should be active during all the cycles of effective value.
    val hcfg = input.payload.config
    val fft2_in_flow = Flow(Vec(HComplex(hcfg), point))
    val data_in_row = Vec( History(input.payload, point, input.valid).reverse )
    fft2_in_flow.payload := data_in_row
    fft2_in_flow.valid := input.valid.lasting(point).last
    val fft_config = FFTConfig(hcfg, point, row, row_pipeline = true, col_pipeline = true)
    val fft2d_core = FFT2D(fft_config)
    fft2d_core.io.line_in <> fft2_in_flow
    fft2d_core.io.line_out
  }

  /**
   * Do fft2d for an image. Input one pixel per cycle, while activating `input.valid`
   *
   * @param input input data flow carry with `valid` and `payload` data. The data is a
   *              HComplex data that represent a pixel of image.
   * @param inverse input signal to control whether do ifft2d for input image or fft2d.
   * @param row indicate the total row number of the input image
   * @param point indicate the total column number of one row of the input image
   * @return output one row per cycle
   */
  def fft2(input: Flow[HComplex], inverse: Bool, row: Int, point: Int): Flow[Vec[HComplex]] = {
    // The valid of input should be active during all the cycles of effective value.
    val hcfg = input.payload.config
    val fft2_in_flow = Flow(Vec(HComplex(hcfg), point))
    fft2_in_flow.valid := input.valid.lasting(point).last
    fft2_in_flow.payload := Vec(History(input.payload, point, input.valid).reverse.map{hcomp=>
      inverse ? hcomp.conj | hcomp
    })
    val fft_config = FFTConfig(hcfg, point, row)
    val fft2d_core = FFT2D(fft_config)
    fft2d_core.io.line_in <> fft2_in_flow
    fft2d_core.io.line_out.translateWith(
      Vec(fft2d_core.io.line_out.payload.map{hcomp=>
        inverse ? hcomp.conj | hcomp
      })
    )
  }
}
