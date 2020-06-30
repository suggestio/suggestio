package com.resumablejs

import org.scalajs.dom.ext.Ajax

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 8:55
  * Description: [[Resumable]] ConfigurationHash.
  * @see [[https://github.com/23/resumable.js/blob/master/resumable.d.ts#L7]]
  */
trait ResumableOptions extends js.Object {

  /** The target URL for the multipart POST request.
    * This can be a string or a function. If a function, it will be passed a FlowFile, a FlowChunk and isTest boolean (Default: /)
    * (Default: /)
    */
  val target: js.UndefOr[String | js.Function3[ResumableFile, ResumableChunk, /*isTest:*/Boolean, String]] = js.undefined

  /** Enable single file upload. Once one file is uploaded, second file will overtake existing one, first one will be canceled.
    * Default: false
    */
  val singleFile: js.UndefOr[Boolean] = js.undefined

  /** The size in bytes of each uploaded chunk of data.
    * The last uploaded chunk will be at least this size and up to two the size, see Issue #51 for details and reasons.
    * (Default: 1*1024*1024)
    */
  val chunkSize: js.UndefOr[Int | js.Function1[ResumableFile, Int]] = js.undefined

  /** Force all chunks to be less or equal than chunkSize.
    * Otherwise, the last chunk will be greater than or equal to chunkSize.
    * (Default: false)
    */
  val forceChunkSize: js.UndefOr[Boolean] = js.undefined

  /** Number of simultaneous uploads.
    * (Default: 3)
    */
  val simultaneousUploads: js.UndefOr[Int] = js.undefined

  /** The name of the multipart POST parameter to use for the file chunk
    * (Default: file)
    **/
  val fileParameterName : js.UndefOr[String] = js.undefined

  /** The name of the chunk index (base-1) in the current upload POST parameter to use for the file chunk.
    * (Default: resumableChunkNumber)
    */
  //val chunkNumberParameterName: js.UndefOr[String] = js.undefined

  /** The name of the total number of chunks POST parameter to use for the file chunk.
    * (Default: resumableTotalChunks)
    */
  //val totalChunksParameterName: js.UndefOr[String] = js.undefined

  /** The name of the general chunk size POST parameter to use for the file chunk.
    * (Default: resumableChunkSize)
    */
  //val chunkSizeParameterName: js.UndefOr[String] = js.undefined

  /** The name of the total file size number POST parameter to use for the file chunk.
    * (Default: resumableTotalSize)
    */
  //val totalSizeParameterName: js.UndefOr[String] = js.undefined

  /** The name of the unique identifier POST parameter to use for the file chunk.
    * (Default: resumableIdentifier)
    */
  //val identifierParameterName: js.UndefOr[String] = js.undefined

  /** The name of the original file name POST parameter to use for the file chunk.
    * (Default: resumableFilename)
    */
  //val fileNameParameterName: js.UndefOr[String] = js.undefined

  /** The name of the file's relative path POST parameter to use for the file chunk.
    * (Default: resumableRelativePath)
    */
  //val relativePathParameterName: js.UndefOr[String] = js.undefined

  /** The name of the current chunk size POST parameter to use for the file chunk.
    * (Default: resumableCurrentChunkSize)
    */
  //val currentChunkSizeParameterName: js.UndefOr[String] = js.undefined

  /** The name of the file type POST parameter to use for the file chunk.
    * (Default: resumableType)
    */
  //val typeParameterName: js.UndefOr[String] = js.undefined

  /** Extra parameters to include in the multipart POST with data.
    * This can be an object or a function.
    * If a function, it will be passed a FlowFile, a FlowChunk object and a isTest boolean.
    * (Default: {})
    */
  val query: js.UndefOr[js.Object | js.Function3[ResumableFile, ResumableChunk, /*isTest:*/Boolean, js.Dictionary[js.Any]]] = js.undefined

  /** Method for chunk test request. (Default: 'GET') */
  val testMethod: js.UndefOr[String] = js.undefined

  /** Method for chunk upload request. (Default: 'POST') */
  val uploadMethod: js.UndefOr[String] = js.undefined

  /** Once a file is uploaded, allow reupload of the same file.
    * By default, if a file is already uploaded, it will be skipped unless the file is removed from the existing Flow object.
    * (Default: false)
    */
  val allowDuplicateUploads: js.UndefOr[Boolean] = js.undefined

  /** Extra prefix added before the name of each parameter included in the multipart POST or in the test GET.
    * (Default: "")
    */
  //val parameterNamespace: js.UndefOr[String] = js.undefined

  /** Extra headers to include in the multipart POST with data.
    * This can be an object or a function that allows you to construct and return a value, based on supplied file.
    * (Default: {})
    */
  val headers: js.UndefOr[js.Dictionary[String] | js.Function1[ResumableFile, js.Dictionary[String]]] = js.undefined

  /** Method to use when POSTing chunks to the server (multipart or octet). (Default: multipart) **/
  val method: js.UndefOr[ResumableOptions.Method] = js.undefined

  /** Prioritize first and last chunks of all files.
    * This can be handy if you can determine if a file is valid for your service from only the first or last chunk.
    * For example, photo or video meta data is usually located in the first part of a file,
    * making it easy to test support from only the first chunk.
    * (Default: false)
    */
  val prioritizeFirstAndLastChunk: js.UndefOr[Boolean] = js.undefined

  /** Make a GET request to the server for each chunks to see if it already exists.
    * If implemented on the server-side, this will allow for upload resumes even after a browser crash or even a computer restart.
    * (Default: true)
    */
  val testChunks: js.UndefOr[Boolean] = js.undefined

  /** Optional function to change Raw Data just before the XHR Request can be sent for each chunk.
    * To the function, it will be passed the chunk and the data as a Parameter.
    * Return the data which will be then sent to the XHR request without further modification.
    * (Default: null).
    */
  val changeRawDataBeforeSend: js.UndefOr[js.Function1[Ajax.InputData, Ajax.InputData]] = js.undefined

  /** Optional function to initialize the fileObject.
    * To the function it will be passed a FlowFile and a FlowChunk arguments.
    */
  val initFileFn: js.UndefOr[js.Function2[ResumableFile, ResumableChunk, js.Object]] = js.undefined

  /** Optional function wrapping reading operation from the original file.
    * To the function it will be passed the FlowFile, the startByte and endByte, the fileType and the FlowChunk. */
  val readFileFn: js.UndefOr[js.Function5[ResumableFile, Double, Double, String, ResumableChunk, js.Any]] = js.undefined

  /** Optional function to process each chunk before testing & sending.
    * Function is passed the chunk as parameter, and should call the preprocessFinished method on the chunk when finished.
    * (Default: null)
    */
  val preprocess: js.UndefOr[js.Function1[ResumableChunk, Unit]] = js.undefined

  /** Override the function that generates unique identifiers for each file. (Default: null) */
  val generateUniqueIdentifier: js.UndefOr[js.Function0[String]] = js.undefined

  /** Indicates how many files can be uploaded in a single session.
    * Valid values are any positive integer and undefined for no limit. (Default: undefined)
    */
  val maxFiles: js.UndefOr[Int] = js.undefined

  /** A function which displays the please upload n file(s) at a time message.
    * (Default: displays an alert box with the message Please n one file(s) at a time.)
    */
  val maxFilesErrorCallback: js.UndefOr[js.Function2[ResumableFile, /*errorCount: */Int, Unit]] = js.undefined

  /** The minimum allowed file size. (Default: undefined) */
  val minFileSize: js.UndefOr[Boolean] = js.undefined

  /** A function which displays an error a selected file is smaller than allowed.
    * (Default: displays an alert for every bad file.)
    */
  val minFileSizeErrorCallback: js.UndefOr[js.Function2[ResumableFile, /*errorCount: */Int, Unit]] = js.undefined

  /** The maximum allowed file size. (Default: undefined) */
  val maxFileSize: js.UndefOr[Boolean] = js.undefined

  /** A function which displays an error a selected file is larger than allowed.
    * (Default: displays an alert for every bad file.)
    */
  val maxFileSizeErrorCallback: js.UndefOr[js.Function2[ResumableFile, /*errorCount:*/ Int, Unit]] = js.undefined

  /** The file types allowed to upload. An empty array allow any file type. (Default: []) */
  val fileType: js.UndefOr[js.Array[String]] = js.undefined

  /** A function which displays an error a selected file has type not allowed.
    * (Default: displays an alert for every bad file.)
    */
  val fileTypeErrorCallback: js.UndefOr[js.Function2[ResumableFile, /*errorCount:*/ Int, Unit]] = js.undefined

  /** The maximum number of retries for a chunk before the upload is failed.
    * Valid values are any positive integer and undefined for no limit. (Default: undefined)
    */
  val maxChunkRetries: js.UndefOr[Int] = js.undefined

  /** The number of milliseconds to wait before retrying a chunk on a non-permanent error.
    * Valid values are any positive integer and undefined for immediate retry. (Default: undefined)
    */
  val chunkRetryInterval: js.UndefOr[Double] = js.undefined

  /** The time interval in milliseconds between progress reports.
    * Set it to 0 to handle each progress callback. */
  val progressCallbacksInterval: js.UndefOr[Double] = js.undefined

  /** Used for calculating average upload speed. Number from 1 to 0.
    * Set to 1 and average upload speed wil be equal to current upload speed.
    * For longer file uploads it is better set this number to 0.02, because time remaining estimation will be more accurate.
    * This parameter must be adjusted together with progressCallbacksInterval parameter.
    * (Default 0.1)
    */
  val speedSmoothingFactor: js.UndefOr[Double] = js.undefined

  /** Response is success if response status is in this list.
    * (Default: [200,201, 202])
    */
  val successStatuses: js.UndefOr[js.Array[Int]] = js.undefined

  /** Response fails if response status is in this list.
    * (Default: [404, 415, 500, 501])
    */
  val permanentErrors: js.UndefOr[js.Array[Int]] = js.undefined

  /** Standard CORS requests do not send or set any cookies by default.
    * In order to include cookies as part of the request, you need to set the withCredentials property to true.
    * (Default: false)
    */
  val withCredentials: js.UndefOr[Boolean] = js.undefined

  val setChunkTypeFromFile: js.UndefOr[Boolean] = js.undefined

}


object ResumableOptions {

  type Method <: js.Any with String
  object Methods {
    final def MULTIPART = "multipart".asInstanceOf[Method]
    final def OCTET     = "octet".asInstanceOf[Method]
  }

}
