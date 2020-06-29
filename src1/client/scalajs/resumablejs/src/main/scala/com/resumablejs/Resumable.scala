package com.resumablejs

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 8:52
  * Description: Resumable API.
  */
@js.native
@JSImport(PACKAGE_NAME, JSImport.Namespace)
class Resumable( val opts: ResumableOptions ) extends js.Object {

  val support: Boolean = js.native
  val files: js.Array[js.Any] = js.native
  val defaults: ResumableOptions = js.native

  val events: js.Array[js.Any] = js.native
  val version: Double = js.native

  /** Assign a browse action to one or more DOM nodes. Pass in true to allow directories to be selected (Chrome only). **/
  def assignBrowse(domNode: dom.html.Element, isDirectory: Boolean): Unit = js.native
  def assignBrowse(domNodes: js.Array[dom.html.Element], isDirectory: Boolean): Unit = js.native

  /** Assign one or more DOM nodes as a drop target. **/
  def assignDrop(domNode: dom.html.Element): Unit = js.native
  def assignDrop(domNodes: js.Array[dom.html.Element]): Unit = js.native
  def unAssignDrop(domNode: dom.html.Element): Unit = js.native
  def unAssignDrop(domNodes: js.Array[dom.html.Element]): Unit = js.native

  /** Start or resume uploading. **/
  def upload(): Unit = js.native
  def uploadNextChunk(): Unit = js.native

  /** Pause uploading. **/
  def pause(): Unit = js.native

  /** Cancel upload of all ResumableFile objects and remove them from the list. **/
  def cancel(): Unit = js.native

  def fire(event: String, args: js.Any*): Unit = js.native

  /** Returns a float between 0 and 1 indicating the current upload progress of all files. **/
  def progress(): Double = js.native

  /** Returns a boolean indicating whether or not the instance is currently uploading anything. **/
  def isUploading(): Boolean = js.native

  /** Add a HTML5 File object to the list of files. **/
  def addFile(file: dom.File, event: dom.Event = js.native): Unit = js.native
  def addFiles(file: dom.FileList, event: dom.Event = js.native): Unit = js.native

  /** Cancel upload of a specific ResumableFile object on the list from the list. **/
  def removeFile(file: ResumableFile): Unit = js.native

  /** Look up a ResumableFile object by its unique identifier. **/
  def getFromUniqueIdentifier(uniqueIdentifier: String): ResumableFile = js.native

  /** Returns the total size of the upload in bytes. **/
  def getSize(): Double = js.native
  def getOpt(o: String): js.Any = js.native


  // Events

  /** Change event handler. **/
  def handleChangeEvent(e: dom.UIEvent): Unit = js.native

  /** Drop event handler. **/
  def handleDropEvent(e: dom.UIEvent): Unit = js.native

  def on(event: String, cb: js.Function): Unit = js.native

}

object Resumable {

  object Events {
    final def FILE_SUCCESS      = "fileSuccess"
    final def FILE_PROGRESS     = "fileProgress"
    final def FILE_ADDED        = "fileAdded"
    final def FILES_ADDED       = "filesAdded"
    final def FILE_RETRY        = "fileRetry"
    final def FILE_ERROR        = "fileError"
    final def UPLOAD_START      = "uploadStart"
    final def COMPLETE          = "complete"
    final def PROGRESS          = "progress"
    final def ERROR             = "error"
    final def PAUSE             = "pause"
    final def BEFORE_PAUSE      = "beforeCancel"
    final def CANCEL            = "cancel"
    final def CHUNKING_START    = "chunkingStart"
    final def CHUNKING_PROGRESS = "chunkingProgress"
    final def CHUNKING_COMPLETE = "chunkingComplete"
    final def CATCH_ALL         = "catchAll"
  }


  implicit final class ResumableOpsExt( private val rsmbl: Resumable ) extends AnyVal {

    // Event helpers
    // @see [[https://github.com/23/resumable.js/blob/master/resumable.d.ts#L234]]

    def onFileSuccess(cb: js.Function1[ResumableFile, Unit]) =
      rsmbl.on( Events.FILE_SUCCESS, cb )

    def onFileProgress(cb: js.Function1[ResumableFile, Unit]) =
      rsmbl.on( Events.FILE_PROGRESS, cb )

    def onFileAdded(cb: js.Function2[ResumableFile, js.UndefOr[dom.DragEvent], Unit]) =
      rsmbl.on( Events.FILE_ADDED, cb )

    def onFilesAdded(cb: js.Function1[js.Array[ResumableFile], Unit]) =
      rsmbl.on( Events.FILES_ADDED, cb )

    def onFileRetry(cb: js.Function1[ResumableFile, Unit]) =
      rsmbl.on( Events.FILE_RETRY, cb )

    def onFileError(cb: js.Function2[ResumableFile, String, Unit]) =
      rsmbl.on( Events.FILE_ERROR, cb )

    def onUploadStart(cb: js.Function0[Unit]) =
      rsmbl.on( Events.UPLOAD_START, cb )

    def onComplete(cb: js.Function0[Unit]) =
      rsmbl.on( Events.COMPLETE, cb )

    def onProgress(cb: js.Function0[Unit]) =
      rsmbl.on( Events.PROGRESS, cb )

    def onError(cb: js.Function0[Unit]) =
      rsmbl.on( Events.ERROR, cb )

    def onPause(cb: js.Function0[Unit]) =
      rsmbl.on( Events.PAUSE, cb )

    def onBeforeCancel(cb: js.Function0[Unit]) =
      rsmbl.on( Events.BEFORE_PAUSE, cb )

    def onCancel(cb: js.Function0[Unit]) =
      rsmbl.on( Events.CANCEL, cb )

    def onChunkingStart(cb: js.Function1[ResumableFile, Unit]) =
      rsmbl.on( Events.CHUNKING_START, cb )

    def onChunkingProgress(cb: js.Function2[ResumableFile, Double, Unit]) =
      rsmbl.on( Events.CHUNKING_PROGRESS, cb )

    def onChunkingComplete(cb: js.Function1[ResumableFile, Unit]) =
      rsmbl.on( Events.CHUNKING_COMPLETE, cb )

    def onCatchAll(cb: js.Function0[Unit]) =
      rsmbl.on( Events.CATCH_ALL, cb )

  }

}
