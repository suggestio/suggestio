package com.github.flowjs.core

import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 13:32
  * Description: ResumableFile API.
  */
@js.native
trait FlowjsFile extends js.Object {

  val flowObj: Flowjs = js.native
  val file: dom.File = js.native
  val name: String = js.native
  val relativePath: String = js.native
  val size: Double = js.native
  val uniqueIdentifier: String = js.native
  /** Average upload speed, bytes per second. */
  val averageSpeed: Double = js.native
  val currentSpeed: Double = js.native
  val chunks: js.Array[FlowjsChunk] = js.native
  val paused: Boolean = js.native
  val error: Boolean = js.native

  /** Returns a float between 0 and 1 indicating the current upload progress of the file.
    * If relative is true, the value is returned relative to all files in the Resumable.js instance. */
  def progress(relative: Boolean): Double = js.native
  def pause(): Unit = js.native
  def resume(): Unit = js.native
  def cancel(): Unit = js.native
  def retry(): Unit = js.native
  /** Rebuild the state of a ResumableFile object, including reassigning chunks and XMLHttpRequest instances. **/
  def bootstrap(): Unit = js.native
  def isUploading(): Boolean = js.native
  def isComplete(): Boolean = js.native

  def sizeUploaded(): Double = js.native
  def timeRemaining(): Double = js.native
  def getExtension(): String = js.native
  def getType(): String = js.native

}
