package NoC

import scala.language._
import spinal.core._

case class FlitConfig(
                     qosWidth: Int = 4,
                     modeWidth: Int = 2,
                     nodeIdWidth: Int = 7, // 7 9 11 13 , why ?
                     txnIdWidth: Int = 8
                     ) {
  def attributeWidth = qosWidth + modeWidth + nodeIdWidth + txnIdWidth
  def portRange = 0 downto 0

  def dirWidth = (nodeIdWidth-1)/2
//  def xDirectionRange =
}
