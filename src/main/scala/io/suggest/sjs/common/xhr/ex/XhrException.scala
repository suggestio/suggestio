package io.suggest.sjs.common.xhr.ex

import org.scalajs.dom.{Event, ErrorEvent, XMLHttpRequest}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:27
 * Description: Исключения при исполнении XHR-запроса (без jquery или иных помошников).
 */

trait XhrException extends RuntimeException {
  def xhr: XMLHttpRequest
}

/** Ошибка связи при выполнении ajax-запроса. */
case class XhrNetworkException(evt: ErrorEvent, xhr: XMLHttpRequest) extends XhrException {

  override def getMessage: String = {
    "XHR failed: " + evt.message
  }

}


/** Наступил таймаут при исполнении XHR-запроса. */
case class XhrTimeoutException(evt: Event, xhr: XMLHttpRequest, timeoutMs: Long) extends XhrException {

  override def getMessage: String = {
    "XHR timeout: " + timeoutMs + " ms"
  }

}


/** Если включена фильтрация по http-статусу ответа сервера, то будет этот экзепшен при недопустимом статусе. */
case class XhrUnexpectedRespStatusException(xhr: XMLHttpRequest) extends XhrException {

  override def getMessage: String = {
    "Unexpected XHR resp status: " + xhr.status
  }

}

