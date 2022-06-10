package NoC

import scala.language._
import spinal.core._

case class FlitConfig(
                     packetIdWidth: Int = 4,
                     packetLenWidth: Int = 4,
                     packetTypeWidth: Int = 3,
                     flitIdWidth: Int = 4,
                     qosWidth: Int = 4, useQos: Boolean = false,
                     modeWidth: Int = 2, useMode: Boolean = false,
                     nodeIdWidth: Int = 7, // 7 9 11 13 , why ?
                     txnIdWidth: Int = 8, useTxn: Boolean = false
                     ) {
  def attributeWidth = qosWidth + modeWidth + nodeIdWidth + txnIdWidth
  def portRange = 0 downto 0

  def dirWidth = (nodeIdWidth-1)/2
//  def xDirectionRange =
}
