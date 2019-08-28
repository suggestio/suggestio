package io.suggest.proto.http.model

import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.cache.MHttpCacheInfo
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
  def headersJsonAccept = Map(
    HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_JSON
  )

  def headersBinarySendAccept =
    headersSendAccept( MimeConst.APPLICATION_OCTET_STREAM )
  def headersBinarySend = Map(
    HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_OCTET_STREAM
  )
  def headersBinaryAccept = Map(
    HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_OCTET_STREAM
  )

  def headersJsonSend = Map(
    HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON
  )

  /** Хидеры, обозначающие что отсылается json и ожидается json в ответ. */
  def headersJsonSendAccept: Map[String, String] =
    headersSendAccept(MimeConst.APPLICATION_JSON)
  def headersSendAccept(mime: String): Map[String, String] =
    headersSendAccept(mime, mime)
  def headersSendAccept(sendMime: String, acceptMime: String): Map[String, String] = {
    val H = HttpConst.Headers
    Map(
      H.CONTENT_TYPE -> sendMime,
      H.ACCEPT       -> acceptMime,
    )
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
  */
case class HttpReqData(
                        headers       : Map[String, String]   = Map.empty,
                        body          : Ajax.InputData        = null,
                        timeoutMs     : Option[Int]           = None,
                        respType      : HttpRespType          = HttpRespTypes.Default,
                        cache         : MHttpCacheInfo        = MHttpCacheInfo.default,
                      ) {

  def timeoutMsOr0 = timeoutMs getOrElse 0

  /** Whether or not cross-site Access-Control requests should be made using credentials such as cookies,
    * authorization headers or TLS client certificates.
    * Also used to indicate when cookies are to be ignored in the response.
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/withCredentials]]
    */
  def xhrWithCredentialsCrossSite: Boolean = false

}
