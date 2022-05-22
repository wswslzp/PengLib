package NoC.Mesh

import NoC._
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class BufferLessRouter[T <: Data](xPos: Int, yPos: Int, config: RouterConfig[T]) extends RouterBase(config) with MeshType[T] {
  import MeshType._
  override def localX = U(xPos, 8 bit)
  override def localY = U(yPos, 8 bit)

  //  logic elements
  case class Crosser(xTurn: FlitAttribute=> Bool, yTurn: FlitAttribute=> Bool) extends Area {
    val xin, yin = Flow(Flit(config.flitConfig, config.dataType))
    val xout, yout = Flow(Flit(config.flitConfig, config.dataType))
    val xinTurn = xin.valid && xTurn(xin.attribute)
    val yinTurn = yin.valid && yTurn(yin.attribute)
    val state = (xin.valid, xinTurn, yin.valid, yinTurn).asBits
    xout << xin ; yout << yin
    when(state === B"1111" || state === B"1100" || state === B"0011") {
      xout << yin; yout << xin
    }
  }
  case class Slicer(turn: FlitAttribute=> Bool, inS: Boolean, outS: Boolean) extends Area {
    val inFlow, outFlow = Flow(Flit(config.flitConfig, config.dataType))
    val inStream = inS generate Stream(Flit(config.flitConfig, config.dataType))
    val outStream = outS generate Stream(Flit(config.flitConfig, config.dataType))
    val scheduleFlow = Flit.firstSchedule(inFlow, inStream).m2sPipe(holdPayload = true)
    val turnValid = if(outS) {
      turn(scheduleFlow.attribute) && outStream.ready
    } else False
    if(outS) outStream << scheduleFlow.takeWhen(turnValid).toStream
    outFlow << scheduleFlow.takeWhen(!turnValid)
  }

  // logic implementation
  if(config.portNum != 4) for(i <- 0 until config.portNum) yield new AreaRoot {
    val inPipe = io.meshIO(i).input.m2sPipe(holdPayload = true)
    val slicer = Slicer(toLocal, inS = true, outS = true)
    slicer.inFlow << inPipe
    slicer.inStream << io.reqIO(i).input
    slicer.outFlow.m2sPipe(holdPayload = true) >> io.meshIO(i).output
    slicer.outStream >/-> io.reqIO(i).output
  }
  else new AreaRoot {
    def nullTurn(attribute: FlitAttribute): Bool = True
    val rx = for(i <- 0 until config.portNum) yield new Area {
      val inPipe = Slicer(toLocal, inS = false, outS = true)
      inPipe.inFlow << io.meshIO(i).input
      inPipe.outStream >/-> io.reqIO(i).output
      val rxDisp = Slicer(nullTurn, inS = true, outS = false) // todo: remove null
      rxDisp.inFlow << inPipe.outFlow
      rxDisp.inStream <-/< io.reqIO(i).input
      val output = rxDisp.outFlow
    }
    val cross = Array.tabulate(config.portNum)(i=> Crosser(crossRouteX(i), crossRouteY(i)))

    cross(EAST).xin  << rx(EAST).output
    cross(WEST).xin  << rx(WEST).output
    cross(NORTH).yin << rx(NORTH).output
    cross(SOUTH).yin << rx(SOUTH).output

    cross(EAST).yin  <-< cross(NORTH).yout
    cross(WEST).yin  <-< cross(SOUTH).yout
    cross(NORTH).xin <-< cross(WEST).xout
    cross(SOUTH).xin <-< cross(EAST).xout

    io.meshIO(EAST).output  <-< cross(NORTH).xout
    io.meshIO(WEST).output  <-< cross(SOUTH).xout
    io.meshIO(NORTH).output <-< cross(WEST).yout
    io.meshIO(SOUTH).output <-< cross(EAST).yout
  }
}