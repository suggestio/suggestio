package io.suggest.pick

import java.nio.ByteBuffer

import io.suggest.bin.{ConvCodecs, IDataConv}
import io.suggest.common.html.HtmlConstants
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpReq, HttpReqData, HttpRespTypes}
import io.suggest.text.{CharSeqUtil, StringUtil}
import japgolly.univeq._
import org.scalajs.dom
import org.scalajs.dom.raw.BlobPropertyBag

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 10:32
  * Description: Утиль для поддержки base64.
  */
object BlobJsUtil extends Log {

  /** scala.js-only base64-декодер в рамках интерфейса конвертеров данных. */
  implicit case object SjsBase64JsDecoder extends IDataConv[String, ConvCodecs.Base64, ByteBuffer] {
    override def convert(base64: String): ByteBuffer = {
      val arr = JsBinaryUtil.base64DecToArr(base64)
      TypedArrayBuffer.wrap(arr.buffer)
    }
  }


  /** Конвертация base64-строки в блоб.
    * Возникла из-за необходимости приведения quill-выхлопов к более человеческому формату данных.
    *
    * Метод асинхронный, т.к. пытается использовать наиболее оптимальные пути через Fetch API,
    * и лишь при неудачах пытается cделать всё это синхронно на голом JS.
    *
    * - Fetch API поддерживается не всеми браузерами (2017), хотя покрывает большинство вариантов.
    * - XHR + base64 url не работает в хроме (хотя работает Fetch для того же самого, слава имбицилам из гугла!).
    * - Синхронный код на голом JS не такой уж быстрый, и может заблокировать интерфейс на больших файлах.
    *
    * @see [[https://stackoverflow.com/questions/16245767/creating-a-blob-from-a-base64-string-in-javascript]]
    */
  def b64Url2Blob(b64Url: String): Future[dom.Blob] = {
    Try {
      HttpClient
        .execute(
          new HttpReq(
            method  = HttpConst.Methods.GET,
            url     = b64Url,
            data    = HttpReqData(
              respType = HttpRespTypes.Blob,
            ),
          )
        )
        .resultFut
        .flatMap(_.blob())
    }
      .recover { case ex =>
        logger.warn( ErrorMsgs.BASE64_TO_BLOB_FAILED, ex, b64Url )
        Future.failed(ex)
      }
      .get
      .recover { case ex =>
        b64Url2BlobPlainConv( b64Url )
      }
  }


  /** Сконвертить base64-URL в Blob используя голый javascript с минимально-возможным потреблением ресурсов.
    * Content-type можно извлечь из префикса base64-URL.
    *
    * @param b64Url Строка Base64 URL.
    * @return Blob
    *         Экзепшен, если исходник не является корректным base64-URL.
    */
  def b64Url2BlobPlainConv(b64Url: String): dom.Blob = {
    // Распарсить начало base64-URL строки. Оно имеет вид: 'data:image/jpg;base64,/9j/4AA...'
    // Проверяем data:
    val dataPrefix = HtmlConstants.Proto.DATA_
    assert( b64Url.startsWith( dataPrefix ) )

    // Считываем Content-type:
    val dataPrefixLength = dataPrefix.length
    val ct = b64Url.substring( dataPrefixLength, b64Url.indexOf(';') )
    assert(ct.length > 0 && ct.length < 40)

    // Проверяем base64-суффикс перед телом
    val b64SuffixExpected = MimeConst.Words.BASE64
    val b64SuffixStart = dataPrefixLength + ct.length + 1
    assert {
      b64SuffixExpected ==* b64Url.substring(
        b64SuffixStart,
        b64Url.indexOf(',')
      )
    }

    // Экстрактим payload. Благодаря CharSequence view, здесь O(1) вместо прожорливого substring().
    val payloadChseq = CharSeqUtil.view(b64Url, b64SuffixStart + b64SuffixExpected.length + 1)
    val uarr = JsBinaryUtil.cleanBase64DecToArr(payloadChseq)
    new dom.Blob(
      js.Array( uarr ),
      BlobPropertyBag( ct )
    )
  }


  /** Заполнить blob дополнительными полями, чтобы оно выглядело как файл.
    *
    * @param blob Исходный блоб.
    * @param fileName Название файла или рандомное.
    * @return
    */
  def ensureBlobAsFile(blob: dom.Blob, fileName: => String = StringUtil.randomId()): dom.File = {
    val blobFile2 = blob.asInstanceOf[dom2.Dom2FileVars]

    // TODO Заиплементить основную реализацию через File Constructor (но он не везде ранее был доступен).
    // Грязный патчинг по принципу https://stackoverflow.com/a/29390393
    if (blobFile2.name.isEmpty)
      blobFile2.name = fileName

    if (blobFile2.lastModifiedDate.isEmpty)
      blobFile2.lastModifiedDate = js.Date.now()

    blobFile2.asInstanceOf[dom.File]
  }


}
