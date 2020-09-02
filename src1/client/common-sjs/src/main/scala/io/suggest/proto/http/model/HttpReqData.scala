package io.suggest.proto.http.model

import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.cache.MHttpCacheInfo
import io.suggest.up.ITransferProgressInfo
import japgolly.univeq.UnivEq
import org.scalajs.dom.ext.Ajax

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 12:46
  * Description: Модель опциональных данных реквеста.
  */

object HttpReqData {

  def empty = apply()

  @inline implicit def univEq: UnivEq[HttpReqData] = {
    import io.suggest.ueq.JsUnivEqUtil._
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  /** Короткая сборка данных json-реквеста без тела для запросов JSON с сервера. */
  def justAcceptJson = apply(
    headers = headersJsonAccept
  )

  /** Хидеры запроса без тела, ожидается json в ответе. */
  def headersJsonAccept = Map.empty[String, String] + (
    HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_JSON
  )

  def headersBinarySendAccept =
    headersSendAccept( MimeConst.APPLICATION_OCTET_STREAM )
  def headersBinarySend = Map.empty[String, String] + (
    HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_OCTET_STREAM
  )
  def headersBinaryAccept = Map.empty[String, String] +
    (HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_OCTET_STREAM)


  def headersJsonSend = Map.empty[String, String] +
    (HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON)


  /** Хидеры, обозначающие что отсылается json и ожидается json в ответ. */
  def headersJsonSendAccept: Map[String, String] =
    headersSendAccept(MimeConst.APPLICATION_JSON)
  def headersSendAccept(mime: String): Map[String, String] =
    headersSendAccept(mime, mime)
  def headersSendAccept(sendMime: String, acceptMime: String): Map[String, String] = {
    val H = HttpConst.Headers
    Map.empty[String, String] +
      (H.CONTENT_TYPE -> sendMime) +
      (H.ACCEPT       -> acceptMime)
  }

  /** Дефолтовые служебные хидеры.
    *
    * @param xrwValue Значение для X-Request-With.
    * @return Карта хидеров.
    */
  def mkBaseHeaders(xrwValue: String = HttpConst.Headers.XRequestedWith.XRW_VALUE): Map[String, String] =
    Map.empty + (HttpConst.Headers.XRequestedWith.XRW_NAME -> xrwValue)


  implicit final class HttpReqDataOpsExt( private val reqData: HttpReqData ) extends AnyVal {

    /** Собрать все заголовки в одну кучу. */
    def allHeaders: Map[String, String] = {
      if (reqData.config.baseHeaders.isEmpty)
        reqData.headers
      else
        reqData.config.baseHeaders ++ reqData.headers

      // config.cookies плюсовать здесь нельзя, т.к. Cookie входит в список prohibited headers.
    }

  }

}


/** Контейнер дополнительных/опциональных данных реквеста.
  * Неявно-пустая модель.
  *
  * @param headers Заголовки реквеста.
  * @param body Тело реквеста, если требуется.
  *             Используется формат Ajax.InputData, который напрямую совместим с Ajax().
  * @param timeoutMs Таймаут. К данным реквеста не относится, но он тут, т.к. в XHR он задаётся до execute().
  * @param respType Тип возвращаемого ответа (для XHR).
  * @param cache Параметры кэширования.
  * @param onProgress  Дёргать функцию по мере прогресса. $1 - 0..100
  * @param credentials Отправлять ли кукисы и прочее при Cross-site запросах?
  *                    None - по умолчанию (same-origin).
  *                    [[https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/withCredentials]]
  * @param config Проброс конфига из разных уровней, где определяются различные константы работы.
  */
case class HttpReqData(
                        headers       : Map[String, String]   = Map.empty,
                        body          : Ajax.InputData        = null,
                        timeoutMs     : Option[Int]           = None,
                        respType      : HttpRespType          = HttpRespTypes.Default,
                        cache         : MHttpCacheInfo        = MHttpCacheInfo.default,
                        onProgress    : Option[ITransferProgressInfo => Unit] = None,
                        config        : HttpClientConfig      = HttpClientConfig.empty,
                        credentials   : Option[Boolean]       = None,
                      ) {

  def timeoutMsOr0 = timeoutMs getOrElse 0

}
