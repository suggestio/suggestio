package com.github.flowjs.core

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 8:52
  * Description: Resumable API.
  */
@js.native
@JSImport(PACKAGE_NAME, JSImport.Namespace)
class Flowjs(val opts: FlowjsOptions ) extends js.Object {

  val support: Boolean = js.native
  val supportDirectory: Boolean = js.native
  val files: js.Array[FlowjsFile] = js.native
  val defaults: FlowjsOptions = js.native

  val events: js.Array[js.Any] = js.native
  val version: Double = js.native

  /** Assign a browse action to one or more DOM nodes. Pass in true to allow directories to be selected (Chrome only). **/
  def assignBrowse(domNode: dom.html.Element | js.Array[dom.html.Element],
                   isDirectory: Boolean,
                   singleFile: Boolean = js.native,
                   attributes: js.Dictionary[String] = js.native,
                  ): Unit = js.native

  /** Assign one or more DOM nodes as a drop target. **/
  def assignDrop(domNode: dom.html.Element | js.Array[dom.html.Element]): Unit = js.native
  def unAssignDrop(domNode: dom.html.Element | js.Array[dom.html.Element]): Unit = js.native

  /** Start or resume uploading. **/
  def upload(): Unit = js.native
  def uploadNextChunk(): Boolean = js.native

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
  def removeFile(file: FlowjsFile): Unit = js.native

  /** Look up a ResumableFile object by its unique identifier. **/
  def getFromUniqueIdentifier(uniqueIdentifier: String): FlowjsFile = js.native

  /** Returns the total size of the upload in bytes. **/
  def getSize(): Double = js.native

  /** @return the total size uploaded of all files in bytes. */
  def sizeUploaded(): Double = js.native

  /** Returns remaining time to upload all files in seconds.
    * Accuracy is based on average speed.
    * If speed is zero, time remaining will be equal to positive infinity Number.POSITIVE_INFINITY
    */
  def timeRemaining(): Double = js.native

  def getOpt(o: String): js.Any = js.native


  // Events

  /** Change event handler. **/
  def handleChangeEvent(e: dom.UIEvent): Unit = js.native

  /** Drop event handler. **/
  def handleDropEvent(e: dom.UIEvent): Unit = js.native

  def on(event: String, cb: js.Function): Unit = js.native

  def off(): Unit = js.native
  def off(event: String): Unit = js.native
  def off(event: String, cb: js.Function): Unit = js.native

}

object Flowjs {

  object Events {
    final def FILE_SUCCESS      = "fileSuccess"
    final def FILE_PROGRESS     = "fileProgress"
    final def FILE_ADDED        = "fileAdded"
    final def FILES_ADDED       = "filesAdded"
    final def FILES_SUBMITTED   = "filesSubmitted"
    final def FILE_REMOVED      = "fileRemoved"
    final def FILE_RETRY        = "fileRetry"
    final def FILE_ERROR        = "fileError"
    final def UPLOAD_START      = "uploadStart"
    final def COMPLETE          = "complete"
    final def PROGRESS          = "progress"
    final def ERROR             = "error"
    final def CATCH_ALL         = "catchAll"
  }


  implicit final class FlowjsOpsExt( private val rsmbl: Flowjs ) extends AnyVal {

    // Event helpers
    // @see [[https://github.com/flowjs/flow.js#events]]

    def onFileSuccess(cb: js.Function3[FlowjsFile, /*message:*/String, FlowjsChunk, Unit]) =
      rsmbl.on( Events.FILE_SUCCESS, cb )

    def onFileProgress(cb: js.Function2[FlowjsFile, FlowjsChunk, Unit]) =
      rsmbl.on( Events.FILE_PROGRESS, cb )

    def onFileAdded(cb: js.Function2[FlowjsFile, js.UndefOr[dom.UIEvent], Unit]) =
      rsmbl.on( Events.FILE_ADDED, cb )

    def onFilesAdded(cb: js.Function2[js.Array[FlowjsFile], js.UndefOr[dom.UIEvent], Unit]) =
      rsmbl.on( Events.FILES_ADDED, cb )

    def onFilesSubmitted(cb: js.Function2[js.Array[FlowjsFile], dom.UIEvent, Unit]): Unit =
      rsmbl.on( Events.FILES_SUBMITTED, cb )

    def onFileRemoved(cb: js.Function1[FlowjsFile, Unit]): Unit =
      rsmbl.on(Events.FILE_REMOVED, cb )

    def onFileRetry(cb: js.Function2[FlowjsFile, FlowjsChunk, Unit]) =
      rsmbl.on( Events.FILE_RETRY, cb )

    /** An error occurred during upload of a specific file. */
    def onFileError(cb: js.Function3[FlowjsFile, String, FlowjsChunk, Unit]) =
      rsmbl.on( Events.FILE_ERROR, cb )

    def onUploadStart(cb: js.Function0[Unit]) =
      rsmbl.on( Events.UPLOAD_START, cb )

    def onComplete(cb: js.Function0[Unit]) =
      rsmbl.on( Events.COMPLETE, cb )

    def onProgress(cb: js.Function0[Unit]) =
      rsmbl.on( Events.PROGRESS, cb )

    def onError(cb: js.Function3[String, FlowjsFile, FlowjsChunk, Unit]) =
      rsmbl.on( Events.ERROR, cb )

    def onCatchAll(cb: js.Function0[Unit]) =
      rsmbl.on( Events.CATCH_ALL, cb )

  }

}
