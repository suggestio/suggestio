package io.suggest.sjs.common.xhr

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.ex._
import org.scalajs.dom.XMLHttpRequest

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 11:14
  * Description: Утиль для поддержки асинхронных запросов.
  *
  * 2016.dec.15: Низкоуровневый код работы с XMLHttpRequest удалён. Теперь просто вызывается scalajs.ext.Ajax().
  * Тут остались только обёртки над штатным Ajax.
  */
object Xhr {

  def MIME_JSON           = "application/json"
  def MIME_TEXT_HTML      = "text/html"
  def MIME_OCTET_STREAM   = "application/octet-stream"

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

  def send(route: Route, timeoutMsOpt: Option[Int] = None,
           headers: TraversableOnce[(String, String)] = Nil, body: Option[js.Any] = None): Future[XMLHttpRequest] = {
    sendRaw(
      method        = route.method,
      url           = route2url(route),
      timeoutMsOpt  = timeoutMsOpt,
      headers       = headers,
      body          = body
    )
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
  def sendRaw(method: String, url: String, timeoutMsOpt: Option[Int] = None,
           headers: TraversableOnce[(String, String)] = Nil, body: Option[js.Any] = None): Future[XMLHttpRequest] = {
    Ajax(
      method = method,
      url    = url,
      data   = body.asInstanceOf[Ajax.InputData],
      timeout = timeoutMsOpt.getOrElse(0),
      headers = headers.toMap,
      withCredentials = false,
      responseType = ""
    )
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

  def route2url(route: Route, preferAbsolute: Boolean = PREFER_ABS_URLS): String = {
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
  def requestJson(route: Route): Future[js.Dynamic] = {
    val xhrFut = successIfStatus(HttpStatuses.OK) {
      send(
        route   = route,
        headers = Seq(HDR_ACCEPT -> MIME_JSON)
      )
    }
    for (xhr <- xhrFut) yield {
      JSON.parse {
        xhr.responseText
      }
    }
  }


  def requestHtml(route: Route): Future[String] = {
    val xhrFut = successIfStatus(HttpStatuses.OK) {
      send(
        route   = route,
        headers = Seq(HDR_ACCEPT -> MIME_TEXT_HTML)
      )
    }
    for (xhr <- xhrFut) yield {
      xhr.responseText
    }
  }

}
