package Pipeline

import spinal.core._
import spinal.lib._
import scala.language._
import scala.collection.mutable

trait PipelinePolicy
object ToolRetime extends PipelinePolicy

/**
 * Auto pipeline feature. We simply set the level of the
 */
abstract class Pipeline extends Module{

  type D <: Data
  private val dataCollection = mutable.HashMap // todo

  /**
   * Check if the module here is a pure data path without loop
   * @return true if the no loop data path
   */
  def checkIfPureDataPath(): Boolean

  override def valCallback[T](ref: T, name: String) = {
    super.valCallback(ref, name)
  }
}
