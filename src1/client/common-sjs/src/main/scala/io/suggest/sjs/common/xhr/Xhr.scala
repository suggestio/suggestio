package io.suggest.sjs.common.xhr

import java.nio.ByteBuffer

import io.suggest.id.IdentConst
import io.suggest.pick.{MimeConst, PickleUtil}
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.model.{HttpRouteExtractor, Route}
import io.suggest.sjs.common.xhr.ex._
import org.scalajs.dom.XMLHttpRequest

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import org.scalajs.dom
import org.scalajs.dom.ext.{Ajax, AjaxException}

import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 11:14
  * Description: Утиль для поддержки асинхронных запросов.
  *
  * 2016.dec.15: Низкоуровневый код работы с XMLHttpRequest удалён. Теперь просто вызывается scalajs.ext.Ajax().
  * Тут остались только обёртки над штатным Ajax.
  */
object Xhr extends Log {

  object RespTypes {
    def ARRAY_BUF = "arraybuffer"
    def ANY       = ""
  }


  private def myProto: Option[String] = {
    Option( dom.window.location )
      .filterNot(js.isUndefined)
      .flatMap( l => Option(l.protocol) )
      .filter { p =>
        !js.isUndefined(p)  &&  p.nonEmpty  &&  p != "null"
      }
      .map(_.toLowerCase())
  }

  private def myHttpProto: Option[String] = {
    myProto.filter { p =>
      p.startsWith( HttpConst.Proto.HTTP )
    }
  }

  /** Флаг предпочтения генерации абсолютных ссылок из Route вместо привычных относительных.
    * Для браузера хватает относительных ссылок, а вот cordova держит webview в локальном контексте. */
  val PREFER_ABS_URLS: Boolean = {
    // Подготовить Xhr к работе. Если cordova-приложение или какой-то локальный запуск, то нужно использовать absoluter urls для реквестов.
    val lOpt = Option( dom.window.location )
      .filterNot(js.isUndefined)

    val isHttp = myHttpProto.nonEmpty

    // Если это не http/https или hostname пустоват, то активировать предпочтетение абсолютных URL.
    val relUrlsOk = isHttp && lOpt
      .flatMap(l => Option(l.hostname))
      .exists(_.nonEmpty)
    !relUrlsOk
  }

  /** Флаг предпочтения https над http при сборки абсолютных ссылок. */
  lazy val PREFER_SECURE_URLS: Boolean = {
    myHttpProto.fold(true) { proto =>
      // Обычно протокол описан как "http:" или "https:". Поэтому просто проверяем наличие буквы s в строке.
      proto.contains("s")
    }
    //println("Xhr.secure = " + r)    // Нельзя тут LOG, иначе будет StackOverflowError во время инициализации RME-логгера.
  }


  def send[HttpRoute: HttpRouteExtractor](route: HttpRoute,
                                          timeoutMsOpt: Option[Int] = None,
                                          headers: TraversableOnce[(String, String)] = Nil,
                                          body: Ajax.InputData = null): Future[XMLHttpRequest] = {
    val hre = implicitly[HttpRouteExtractor[HttpRoute]]
    sendRaw(
      method        = hre.method(route),
      url           = route2url( route ),
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
              headers: TraversableOnce[(String, String)] = Nil, body: Ajax.InputData = null): Future[XMLHttpRequest] = {
    _handleUnauthorized {
      Ajax(
        method = method,
        url    = url,
        data   = body,
        timeout = timeoutMsOpt.getOrElse(0),
        headers = headers.toMap,
        withCredentials = false,
        responseType = ""
      )
    }
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
    successIfStatusF( httpStatuses.contains )(xhrFut)
  }
  def successIf200(xhrFut: Future[XMLHttpRequest]): Future[XMLHttpRequest] = {
    successIfStatus( HttpStatuses.OK )(xhrFut)
  }
  def successIf30(xhrFut: Future[XMLHttpRequest]): Future[XMLHttpRequest] = {
    successIfStatusF(_ / 10 == 30)(xhrFut)
  }
  def successIfStatusF(isOkF: Int => Boolean)(xhrFut: Future[XMLHttpRequest]): Future[XMLHttpRequest] = {
    for (xhr <- xhrFut) yield {
      if ( isOkF(xhr.status) ) {
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

  def route2url[HttpRoute: HttpRouteExtractor](route: HttpRoute, preferAbsolute: Boolean = PREFER_ABS_URLS): String = {
    val hre = implicitly[HttpRouteExtractor[HttpRoute]]
    if (preferAbsolute)
      hre.absoluteUrl( route, PREFER_SECURE_URLS )
    else
      hre.url( route )
  }

  /**
    * HTTP-запрос через js-роутер и ожидание HTTP 200 Ok ответа.
    *
    * @param route Маршрут jsrouter'а. Он содержит данные по URL и METHOD для запроса.
    * @return Фьючерс с десериализованным JSON.
    */
  def requestJson[HttpRoute: HttpRouteExtractor](route: HttpRoute): Future[js.Dynamic] = {
    for (jsonText <- requestJsonText(route)) yield {
      JSON.parse( jsonText )
    }
  }

  /**
    * Запрос JSON сервера без парсинга JSON на клиенте.
    * Метод появился как временный костыль к play-json, который через API парсит только строки.
    */
  def requestJsonText[HttpRoute: HttpRouteExtractor](route: HttpRoute, timeoutMsOpt: Option[Int] = None): Future[String] = {
    val xhrFut = successIf200 {
      send(
        route         = route,
        headers       = Seq(HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_JSON),
        timeoutMsOpt  = timeoutMsOpt
      )
    }
    for (xhr <- xhrFut) yield {
      xhr.responseText
    }
  }

  def requestHtml[HttpRoute: HttpRouteExtractor](route: HttpRoute): Future[String] = {
    val xhrFut = successIf200 {
      send(
        route   = route,
        headers = Seq(HttpConst.Headers.ACCEPT -> MimeConst.TEXT_HTML)
      )
    }
    for (xhr <- xhrFut) yield {
      xhr.responseText
    }
  }

  val RESP_ARRAY_BUFFER = "arraybuffer"
  val RESP_BLOB = "blob"

  /**
    * Запрос бинарщины с сервера. Ответ обычно подхватывается через boopickle.
    *
    * @see По мотивам autowire-клиента из [[https://github.com/ochrons/scalajs-spa-tutorial/blob/290c3f7cb3f0c9168cbb61d2b39cc330a09ebe4c/client/src/main/scala/spatutorial/client/services/AjaxClient.scala#L12]]
    *
    * @param route Роута
    * @param body Опциональное тело запроса.
    * @return Фьючерс с блобом.
    */
  def requestBinary[HttpRoute: HttpRouteExtractor](route: HttpRoute, body: Ajax.InputData = null): Future[ByteBuffer] = {
    respAsBinary {
      successIf200 {
        sendBinary(
          route     = route,
          body      = body,
          respType  = RESP_ARRAY_BUFFER,
          headers   = (HttpConst.Headers.ACCEPT -> MimeConst.APPLICATION_OCTET_STREAM) :: Nil
        )
      }
    }
  }

  def sendBinary[HttpRoute: HttpRouteExtractor](route: HttpRoute, body: Ajax.InputData, respType: String,
                                                headers: List[(String, String)] = Nil): Future[XMLHttpRequest] = {
    _handleUnauthorized {
      val hrex = implicitly[HttpRouteExtractor[HttpRoute]]
      Ajax(
        method          = hrex.method(route),
        url             = hrex.url(route),
        data            = body.asInstanceOf[Ajax.InputData],
        timeout         = 0,
        headers         = ((HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_OCTET_STREAM) :: headers).toMap,
        withCredentials = false,
        responseType    = respType
      )
    }
  }

  /** Повесить на XHR-ответы фоновую слушалку ответов сервера, чтобы отработать случаи завершения пользователькой сессии.
    *
    * @param xhrFut Выхлоп Ajax().
    * @return Фьючерс с XHR внутри.
    */
  private def _handleUnauthorized(xhrFut: Future[XMLHttpRequest]): Future[XMLHttpRequest] = {
    xhrFut.failed.foreach {
      case AjaxException(xhr) =>
        if (xhr.status == HttpConst.Status.UNAUTHORIZED) {
          // 401 в ответе означает, что сессия истекла и продолжать нормальную работу невозможно.
          DomQuick.reloadPage()
        }

      case _ => // Do nothing
    }

    // Повесить слушалку на как-будто-бы-положительные XHR-ответы, чтобы выявлять редиректы из-за отсутствия сессии.
    for (xhr <- xhrFut) {
      if (xhr.status == HttpConst.Status.OK && Option(xhr.getResponseHeader(IdentConst.HTTP_HDR_SUDDEN_AUTH_FORM_RESP)).nonEmpty ) {
        // Пришла HTML-форма в ответе. Такое бывает, когда сессия истекла, но "Accept:" допускает HTML-ответы.
        DomQuick.reloadPage()
      }
    }

    // Вернуть исходный реквест, ибо side-effect'ы работают сами по себе.
    xhrFut
  }

  def respAsBinary(xhrFut: Future[XMLHttpRequest]): Future[ByteBuffer] = {
    for (xhr <- xhrFut) yield {
      TypedArrayBuffer.wrap( xhr.response.asInstanceOf[ArrayBuffer] )
    }
  }


  import boopickle.Default._

  /**
    * Декодировать будущий ответ сервера в инстанс какой-то модели с помощью boopickle и десериализатора,
    * переданного в implicit typeclass'е.
    *
    * @param respFut Фьючерс с ответом сервера.
    * @param u Модуль сериализации boopickle.
    * @tparam T Тип отрабатываемой модели.
    * @return Фьючерс с десериализованным инстансом произвольной модели.
    */
  def unBooPickleResp[T](respFut: Future[ByteBuffer])(implicit u: Pickler[T]): Future[T] = {
    for (bbuf <- respFut) yield {
      PickleUtil.unpickle[T](bbuf)
    }
  }

}
