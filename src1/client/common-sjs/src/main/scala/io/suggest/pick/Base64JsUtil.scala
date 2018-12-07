package io.suggest.pick

import java.nio.ByteBuffer

import io.suggest.bin.{ConvCodecs, IDataConv}
import io.suggest.common.html.HtmlConstants
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.xhr.HttpRespTypes
import io.suggest.text.CharSeqUtil
import japgolly.univeq._
import org.scalajs.dom.Blob
import org.scalajs.dom.experimental.Fetch
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.BlobPropertyBag

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 10:32
  * Description: Утиль для поддержки base64.
  */
object Base64JsUtil {

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
  def b64Url2Blob(b64Url: String): Future[Blob] = {
    b64Url2BlobUsingFetch( b64Url )
      // В хроме не пашет. В нестарых FF есть fetch(). XHR-костыль тут для *возможного* ускорения MSIE, старых FF, Opera, и прочей второсортчины.
      .recoverWith { case _ =>
        b64Url2BlobUsingXHR( b64Url )
      }
      // Самый надежный вариант, и одновременно тормозной и блокирующий.
      .recover { case _ =>
        b64Url2BlobPlainConv( b64Url )
      }
  }


  /** Попытаться сконвертить base64-URL в Blob с помощью Fetch API.
    *
    * @param b64Url base64 URL.
    * @return Фьючерс с блобом.
    *         Future.failed при ошибке, в том числе если Fetch API не поддерживается.
    */
  def b64Url2BlobUsingFetch(b64Url: String): Future[Blob] = {
    try {
      // TODO Использовать Xhr.scala. А внутри Xhr реализовать поддержку Fetch API.
      Fetch
        .fetch(b64Url)
        .toFuture
        .flatMap(_.blob().toFuture)
    } catch {
      case ex: Throwable =>
        // Fetch API может не поддерживаться браузером. Это нормально.
        Future.failed(ex)
    }
  }


  /** Пытаемся сконвертить base64-URL в Blob с помощью XHR.
    * Это работает в Firefox, но синхронно TODO падает в Chrome в момент вызова xhr.send().
    *
    * @param b64Url Строка Base64 URL.
    * @return Фьючерс с блобом.
    *         Future.failed при ошибке, в том числе синхронной ошибке XHR API.
    */
  def b64Url2BlobUsingXHR(b64Url: String): Future[Blob] = {
    try {
      for {
        xhr <- Ajax(
          method  = HttpConst.Methods.GET,
          url     = b64Url,
          data    = null,
          timeout = 0,
          headers = Map.empty,
          withCredentials = false,
          responseType = HttpRespTypes.Blob.xhrResponseType
        )
      } yield {
        assert( xhr.status ==* HttpConst.Status.OK )
        xhr.response.asInstanceOf[Blob]
      }
    } catch {
      case ex: Throwable =>
        Future.failed(ex)
    }
  }


  /** Сконвертить base64-URL в Blob используя голый javascript с минимально-возможным потреблением ресурсов.
    * Content-type можно извлечь из префикса base64-URL.
    *
    * @param b64Url Строка Base64 URL.
    * @return Blob
    *         Экзепшен, если исходник не является корректным base64-URL.
    */
  def b64Url2BlobPlainConv(b64Url: String): Blob = {
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
    new Blob(
      js.Array( uarr ),
      BlobPropertyBag( ct )
    )
  }

}
