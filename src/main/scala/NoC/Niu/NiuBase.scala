package NoC.Niu
import NoC._
import spinal.core._
import spinal.lib._

abstract class NiuBase[T<:Data, B <: Bundle with IMasterSlave](interface: HardType[B], niuConfig: NiuConfig[T], routerConfig: RouterConfig[T], id: Int) extends Module {
  protected def flitPayload : HardType[T]
  val io = new Bundle {
    val bus = slave(interface())
    val request = RequestChannel(routerConfig)
  }

  def connect(reqs: Seq[RequestChannel[T]]): this.type = {
    val outputs = reqs.map(_.output)
    val arbitratedOutput = StreamArbiterFactory.roundRobin.on(outputs)
    this.io.request.input << arbitratedOutput
    val meshInputs = StreamDispatcherSequential(this.io.request.output, routerConfig.portNum)
    reqs.map(_.input).zip(meshInputs).foreach {case(meshReq, niuReq)=> meshReq << niuReq}
    this
  }
}
