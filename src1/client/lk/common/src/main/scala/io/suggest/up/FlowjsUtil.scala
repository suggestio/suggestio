package io.suggest.up


import com.resumablejs.{Resumable, ResumableChunk, ResumableFile, ResumableOptions}
import io.suggest.common.qs.QsConstants
import io.suggest.crypto.asm.HashWwTask
import io.suggest.crypto.hash.HashesHex
import io.suggest.file.MJsFileInfo
import io.suggest.proto.http.HttpConst
import io.suggest.routes.routes
import io.suggest.url.MHostUrl
import io.suggest.xplay.json.PlayJsonSjsUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import play.api.libs.json.Json

import scala.collection.immutable.HashMap
import scala.concurrent.Promise
import scala.scalajs.js
import scala.util.{Success, Try}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.06.2020 14:49
  * Description: Утиль для взаимодействия с resumable.js.
  */
object FlowjsUtil {

  /** Собрать инстанс Resumable.js для UploadApi. */
  def tryFlowJs(uploadHostUrl: MHostUrl): Try[Resumable] = {
    // Узнать, resumable.js в состоянии работать или нет?
    Try {
      lazy val upTgQsStr = QsConstants.DO_NOT_TOUCH_PREFIX + (uploadHostUrl.relUrl.replaceFirst("^[^?]+\\?", ""))

      val chunkHashAlgo = UploadConstants.CleverUp.UPLOAD_CHUNK_HASH
      var chunkHashes = HashMap.empty[Int, String]

      val _targetChunkUrlF: js.Function3[ResumableFile, ResumableChunk, Boolean, String] = {
        (file, chunk, isTest) =>
          // Надо получить на руки данные chunk'а
          val chunkNumber = chunk.chunkNumber
          val chunkHashesHex = chunkHashes
            .get( chunkNumber )
            .fold[HashesHex]( Map.empty ) { chunkHashValue =>
              Map.empty + (chunkHashAlgo -> chunkHashValue)
            }

          val chunkQs = MUploadChunkQs(
            hashesHex    = chunkHashesHex,
            chunkNumberO = None,
          )
          val chunkQsDict = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(chunkQs) )

          // Абсолютизация ссылки:
          HttpConst.Proto.CURR_PROTO +
            uploadHostUrl.host +
            routes.controllers.Upload.chunk(upTgQsStr, chunkQsDict).url
      }

      def __preprocessChunk( chunk: ResumableChunk ): Unit = {
        println("Preprocess chunk: " + chunk)
        val cn = chunk.chunkNumber
        if (chunkHashes contains cn) {
          Try( chunk.preprocessFinished() )
        } else {
          val chunkBlob = chunk.fileObj.file.slice( chunk.startByte, chunk.endByte )

          // blocking: выполнить хэширование одной части: TODO Opt Нужна оптимизация WebWorkers.
          HashWwTask( chunkHashAlgo, chunkBlob )
            .run()
            .andThen { case Success(chunkHashValue) =>
              chunkHashes += (chunk.chunkNumber -> chunkHashValue)
            }
            //.andThen { case _ =>
            //  chunkBlob.close()   // .close is not a function.
            //}
            .andThen { case r =>
              println(s"preprocess chunk#${chunk.chunkNumber} finished: $r")
              Try( chunk.preprocessFinished() )
            }
        }
      }

      val _chunkSizeB = MUploadChunkSizes.default.value

      new Resumable(
        new ResumableOptions {
          override val target = js.defined( _targetChunkUrlF )
          override val singleFile = true
          override val chunkSize = _chunkSizeB
          override val simultaneousUploads = 2
          override val method = ResumableOptions.Methods.OCTET
          override val testChunks = true

          override val uploadMethod = routes.controllers.Upload
            .chunk( "", js.Dictionary.empty )
            .method

          override val testMethod = routes.controllers.Upload
            .hasChunk( "", js.Dictionary.empty )
            .method

          // preprocessChunk(): вычислять хэш-сумму слайс-блоба, закидывать в общую мапу, которая будет использована для сборки ссылки.
          override val preprocess = js.defined( __preprocessChunk )
        }
      )
    }
      .filter(_.support)
  }


  /** Подписаться на события onProgress. */
  def subscribeProgress(rsmbl: Resumable, file: MJsFileInfo, onProgressF: ITransferProgressInfo => Unit): Unit = {
    rsmbl.onProgress { () =>
      val transferProgressInfo = new ITransferProgressInfo {
        override def totalBytes = Some( file.blob.size )
        override def loadedBytes: Double = rsmbl.progress() * file.blob.size
        override def progress = Some( rsmbl.progress() )
      }
      onProgressF( transferProgressInfo )
    }
  }


  /** Подписка на события окончания заливки. */
  def subscribeErrors(rsmbl: Resumable, p: Promise[Unit]): Unit = {
    rsmbl.onError { (msg, file, chunk) =>
      if (!rsmbl.isUploading())
        p.tryFailure( new RuntimeException(msg + ": " + file.uniqueIdentifier + " #" + chunk.chunkNumber) )
    }
  }

  def subscribeComplete(rsmbl: Resumable, p: Promise[Unit]): Unit = {
    rsmbl.onComplete { () =>
      p.trySuccess(())
    }
  }

}
