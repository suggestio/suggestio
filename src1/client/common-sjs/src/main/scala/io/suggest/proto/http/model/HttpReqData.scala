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

  def headersBinarySendAccept = _headersSendAccept( MimeConst.APPLICATION_OCTET_STREAM )
  def headersBinarySend = Map(
    HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_OCTET_STREAM
  )
  def headersBinaryAccept = Map(
    HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_OCTET_STREAM
  )

  /** Хидеры, обозначающие что отсылается json и ожидается json в ответ. */
  def headersJsonSendAccept = _headersSendAccept(MimeConst.APPLICATION_JSON)

  private def _headersSendAccept(mime: String) = {
    val H = HttpConst.Headers
    Map(
      H.ACCEPT       -> mime,
      H.CONTENT_TYPE -> mime
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
  * @param responseType Тип возвращаемого ответа.
  * @param cache Параметры кэширования.
  */
case class HttpReqData(
                        headers       : Map[String, String]   = Map.empty,
                        body          : Ajax.InputData        = null,
                        timeoutMs     : Option[Int]           = None,
                        respType      : HttpRespType          = HttpRespTypes.Default,
                        cache         : MHttpCacheInfo        = MHttpCacheInfo.default,
                      ) {

  def withHeaders(headers: Map[String, String]) = copy(headers = headers)
  def addHeaders(hdrs: (String, String)*) = {
    if (hdrs.isEmpty) this
    else withHeaders(headers ++ hdrs)
  }

  def withBody(body: Ajax.InputData) = copy(body = body)
  def withTimeout(timeoutMs: Option[Int]) = copy(timeoutMs = timeoutMs)

  def timeoutMsOr0 = timeoutMs getOrElse 0

  /** Whether or not cross-site Access-Control requests should be made using credentials such as cookies,
    * authorization headers or TLS client certificates.
    * Also used to indicate when cookies are to be ignored in the response.
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/withCredentials]]
    */
  def xhrWithCredentialsCrossSite: Boolean = false

}
