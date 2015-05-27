package io.suggest.sjs.common.xhr

import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.xhr.ex._
import org.scalajs.dom.raw.ErrorEvent
import org.scalajs.dom.{Event, XMLHttpRequest}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:14
 * Description: Утиль для поддержки асинхронных запросов.
 */
object Xhr extends SjsLogger {

  def MIME_JSON = "application/json"

  /**
   * Отправка асинхронного запроса силами голого js.
   * @see [[http://stackoverflow.com/a/8567149 StackOverflow]]
   * @param method HTTP-метод.
   * @param url Ссылка.
   * @param timeoutMsOpt Таймаут запроса в миллисекундах, если необходимо.
   * @param accept Выставить Accept: заголовок запроса.
   * @return Фьючерс с результатом.
   */
  def send(method: String, url: String, timeoutMsOpt: Option[Long] = None, accept: Option[String] = None): Future[XMLHttpRequest] = {
    val p = Promise[XMLHttpRequest]()
    // Собрать XHR
    val xhr = new XMLHttpRequest()
    xhr.open(method, url, async = true)
    accept.foreach { _accept =>
      xhr.setRequestHeader("Accept", _accept)
    }
    xhr.onload = { (evt: Event) =>
      p success xhr
    }
    xhr.onerror = { (evt: ErrorEvent) =>
      p failure XhrNetworkException(evt, xhr)
    }
    if (timeoutMsOpt.nonEmpty) {
      val t = timeoutMsOpt.get
      xhr.timeout = t
      xhr.ontimeout = { (evt: Event) =>
        p failure XhrTimeoutException(evt, xhr, t)
      }
    }
    // Запустить запрос
    try {
      xhr.send()
    } catch {
      case ex: Throwable =>
        p failure ex
    }
    // Вернуть future.
    p.future
  }


  /**
   * Фильтровать результат по http-статусу ответа сервера.
   * @param httpStatuses Допустимые http-статусы.
   * @param reqFut Фьючерс реквеста, собранного в send().
   * @return Future, где success наступает только при указанных статусах.
   *         [[io.suggest.sjs.common.xhr.ex.XhrUnexpectedRespStatusException]] когда статус ответа не подпадает под критерий.
   */
  def successWithStatus(httpStatuses: Int*)(reqFut: Future[XMLHttpRequest])(implicit ec: ExecutionContext): Future[XMLHttpRequest] = {
    reqFut.flatMap { xhr =>
      if (httpStatuses contains xhr.status) {
        Future successful xhr
      } else {
        Future failed XhrUnexpectedRespStatusException(xhr)
      }
    }
  }

}
