package io.suggest.sjs.common.xhr

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.xhr.ex._
import org.scalajs.dom.raw.ErrorEvent
import org.scalajs.dom.{Event, XMLHttpRequest}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSON
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:14
 * Description: Утиль для поддержки асинхронных запросов.
 */
object Xhr extends SjsLogger {

  def MIME_JSON           = "application/json"
  def MIME_TEXT_HTML      = "text/html"

  def HDR_ACCEPT          = "Accept"
  def HDR_CONTENT_TYPE    = "Content-Type"
  def HDR_CONTENT_LENGHT  = "Content-Lenght"
  def HDR_CONNECTION      = "Connection"


  /** Флаг предпочтения генерации абсолютных ссылок из Route вместо привычных относительных.
    * Для браузера хватает относительных ссылок, а вот cordova держит webview в локальном контексте. */
  val PREFER_ABS_URLS: Boolean = {
    // Подготовить Xhr к работе. Если cordova-приложение или какой-то локальный запуск, то нужно использовать absoluter urls для реквестов.
    val lOpt = Option( dom.window.location )
      .filterNot(js.isUndefined)
    val protoOpt = lOpt
      .flatMap { l =>
        Option(l.protocol)
      }
    val isHttp = protoOpt.exists { proto =>
      proto.toLowerCase.trim.startsWith("http")
    }
    // Если это не http/https или hostname пустоват, то активировать предпочтетение абсолютных URL.
    val relUrlsOk = isHttp && lOpt
      .flatMap(l => Option(l.hostname))
      .exists(_.nonEmpty)
    !relUrlsOk
  }


  /** Флаг предпочтения https над http при сборки абсолютных ссылок. */
  lazy val PREFER_SECURE_URLS: Boolean = {
    Option( dom.window.location )
      .flatMap( l => Option(l.protocol) )
      .filter { p =>
        !js.isUndefined(p)  &&  p.nonEmpty  &&  p != "null"
      }
      .fold(true) {
        case "http:"  => false
        case _        => true
      }
  }


  /**
    * Отправка асинхронного запроса силами голого js.
    *
    * @see [[http://stackoverflow.com/a/8567149 StackOverflow]]
    * @param method HTTP-метод.
    * @param url Ссылка.
    * @param timeoutMsOpt Таймаут запроса в миллисекундах, если необходимо.
    * @return Фьючерс с результатом.
    */
  def send(method: String, url: String, timeoutMsOpt: Option[Long] = None,
           headers: TraversableOnce[(String, String)] = Nil, body: Option[js.Any] = None): Future[XMLHttpRequest] = {
    // Собрать XHR
    val xhr = new XMLHttpRequest()
    xhr.open(method, url, async = true)

    // Запилить хидеры в запрос.
    for ((k, v) <- headers) {
      xhr.setRequestHeader(k, v)
    }

    val p = Promise[XMLHttpRequest]()

    // Отработать возможный timeout.
    timeoutMsOpt.foreach { t =>
      xhr.timeout = t
      xhr.ontimeout = { (evt: Event) =>
        p.failure( XhrTimeoutException(evt, xhr, t) )
      }
    }

    // Повесить стандартные listener'ы, запустить запрос на исполнение.
    xhr.onload = { (evt: Event) =>
      p.success( xhr )
    }
    xhr.onerror = { (evt: ErrorEvent) =>
      p.failure( XhrNetworkException(evt, xhr) )
    }

    // Причёсываем тело, если оно есть.
    val data: js.Any = body.orNull

    // Запустить запрос
    try {
      xhr.send(data)
    } catch {
      case ex: Throwable =>
        p.failure( ex )
    }

    // Вернуть future.
    p.future
  }


  /**
    * Фильтровать результат по http-статусу ответа сервера.
    *
    * @param httpStatuses Допустимые http-статусы.
    * @param xhrFut Выполненяемый XHR, собранный в send().
    * @return Future, где success наступает только при указанных статусах.
    *         [[io.suggest.sjs.common.xhr.ex.XhrUnexpectedRespStatusException]] когда статус ответа не подпадает под критерий.
    */
  def successIfStatus(httpStatuses: Int*)(xhrFut: Future[XMLHttpRequest]): Future[XMLHttpRequest] = {
    for (xhr <- xhrFut) yield {
      if (httpStatuses.contains(xhr.status)) {
        xhr
      } else {
        throw XhrUnexpectedRespStatusException(xhr)
      }
    }
  }


  def someIfStatus(httpStatuses: Int*)(xhrFut: Future[XMLHttpRequest]): Future[Option[XMLHttpRequest]] = {
    for (xhr <- xhrFut) yield {
      if (httpStatuses.contains(xhr.status)) {
        Some(xhr)
      } else {
        None
      }
    }
  }

  def route2url(route: Route, preferAbsolute: Boolean): String = {
    if (preferAbsolute)
      route.absoluteURL( PREFER_SECURE_URLS )
    else
      route.url
  }

  /**
    * HTTP-запрос через js-роутер и ожидание HTTP 200 Ok ответа.
    *
    * @param route Маршрут jsrouter'а. Он содержит данные по URL и METHOD для запроса.
    * @return Фьючерс с десериализованным JSON.
    */
  def requestJson(route: Route, preferAbsUrl: Boolean = PREFER_ABS_URLS): Future[js.Dynamic] = {
    val xhrFut = successIfStatus(HttpStatuses.OK) {
      send(
        method  = route.method,
        url     = route2url( route, preferAbsUrl ),
        headers = Seq(HDR_ACCEPT -> MIME_JSON)
      )
    }
    for (xhr <- xhrFut) yield {
      JSON.parse {
        xhr.responseText
      }
    }
  }

}
