package com.resumablejs

import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 13:37
  * Description: API for ResumableChunk.
  * @see [[https://github.com/23/resumable.js/blob/master/resumable.d.ts#L369]]
  * @see [[https://github.com/23/resumable.js/blob/master/resumable.js#L660]]
  */
@js.native
trait ResumableChunk extends js.Object {

  val opts: js.Object
  def getOpt(o: String): js.Any

  val flowObj: Resumable
  val fileObj: ResumableFile
  val fileObjSize: Double
  val fileObjType: String
  val offset: Int
  val tested: Boolean
  val callback: js.Function
  val lastProgressCallback: js.Date
  val retries: Int
  val pendingRetry: Boolean
  /** 0 = unprocessed, 1 = processing, 2 = finished */
  val preprocessState: Int
  /** 0 = not read, 1 = reading, 2 = finished */
  val readState: Int
  val markComplete: Boolean

  val loaded: Double    // xhr.onProgress
  val total: Double
  val chunkSize: Double
  val filename: String  //or null

  val startByte: Double
  val endByte: Double
  def computeEndByte(): Double

  val xhr: dom.XMLHttpRequest
  def test(): Unit
  def preprocessFinished(): Unit
  def send(): Unit

  def abort(): Unit

  /** @return Returns: 'pending', 'uploading', 'success', 'error' */
  def status(): String

  /** @return xhr.responseText | "" */
  def message(): String
  def progress(relative: Boolean): Double


  // prototype:
  def getParams(): ChunkQsParams

}

object ResumableChunk {

  object PreprocessState {
    final def UNPROCESSED = 0
    final def PROCESSING = 1
    final def FINISHED = 2
  }

  object Status {
    final def PENDING = "pending"
    final def UPLOADING = "uploading"
    final def SUCCESS = "success"
    final def ERROR = "error"
  }


  implicit final class ResumableChunkOpsExt( private val resumChunk: ResumableChunk ) extends AnyVal {

    def chunkNumber: Int =
      resumChunk.offset.toInt + 1

  }

}


/** @see [[https://github.com/flowjs/flow.js/blob/master/src/flow.js#L1281]] */
trait ChunkQsParams extends js.Object {
  val flowChunkNumber: Int
  val flowChunkSize: Int
  val flowCurrentChunkSize: Double
  val flowTotalSize: Double
  val flowIdentifier: String
  val flowFilename: String
  val flowRelativePath: String
  val flowTotalChunks: Int
}
