package com.resumablejs

import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 13:32
  * Description: ResumableFile API.
  * @see [[https://github.com/23/resumable.js/blob/master/resumable.d.ts#L308]]
  */
@js.native
trait ResumableFile extends js.Object {

  val resumableObj: Resumable = js.native
  val file: dom.File = js.native
  val fileName: String = js.native
  val relativePath: String = js.native
  /** Double, because Long is not-opaque, Int limited by 2GB. */
  val size: Double = js.native
  val uniqueIdentifier: String = js.native
  val chunks: js.Array[ResumableChunk] = js.native

  /** Returns a float between 0 and 1 indicating the current upload progress of the file.
    * If relative is true, the value is returned relative to all files in the Resumable.js instance. */
  def progress(relative: Boolean): Double = js.native
  def abort(): Unit = js.native
  def cancel(): Unit = js.native
  def retry(): Unit = js.native
  /** Rebuild the state of a ResumableFile object, including reassigning chunks and XMLHttpRequest instances. **/
  def bootstrap(): Unit = js.native
  def isUploading(): Boolean = js.native
  def isComplete(): Boolean = js.native

}
