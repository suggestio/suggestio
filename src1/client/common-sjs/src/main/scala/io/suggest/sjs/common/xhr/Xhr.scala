package io.suggest.sjs.common.xhr

import io.suggest.id.IdentConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.model.{HttpRoute, HttpRouteExtractor}
import io.suggest.sjs.common.xhr.ex._
import org.scalajs.dom.XMLHttpRequest

import scala.concurrent.Future
import scala.scalajs.js
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import org.scalajs.dom
import org.scalajs.dom.ext.{Ajax, AjaxException}

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

  /** Функция доп.обработки URL для допиливания их до ранга абсолютных, когда это необходимо.
    * Бывает, что требуются строго абсолютные URL (cordova). Тут - собираем фунцкию для причёсывания исходных ссылок.
    */
  val mkAbsUrlIfPreferred: String => String = if (Xhr.PREFER_ABS_URLS) {
    // Фунция на случай, когда требуется причёсывать ссылки:
    val httpProto = HttpConst.Proto.HTTP
    val isSecure = true

    url0: String =>
      HttpRoute.mkAbsUrl( protoPrefix = httpProto, secure = isSecure, relUrl = url0 )

  } else {
    // Причёсывать ссылки не требуется. Просто используем исходные ссылки.
    identity[String]
  }


  /** Флаг предпочтения https над http при сборки абсолютных ссылок. */
  lazy val PREFER_SECURE_URLS: Boolean = {
    myHttpProto.fold(true) { proto =>
      // Обычно протокол описан как "http:" или "https:". Поэтому просто проверяем наличие буквы s в строке.
      proto.contains("s")
    }
    //println("Xhr.secure = " + r)    // Нельзя тут LOG, иначе будет StackOverflowError во время инициализации RME-логгера.
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
  def execute(httpReq: HttpReq): HttpRespHolder = {
    XhrHttpRespHolder(
      xhrFut = _handleUnauthorized {
        Ajax(
          method          = httpReq.method,
          url             = httpReq.url,
          data            = httpReq.data.body,
          timeout         = httpReq.data.timeoutMsOr0,
          headers         = httpReq.data.headers,
          withCredentials = httpReq.data.xhrWithCredentialsCrossSite,
          responseType    = httpReq.data.respType.xhrResponseType,
        )
          .catchAjaxEx(method = httpReq.method, url = httpReq.url)
      }
    )
  }


  def route2url[HttpRoute: HttpRouteExtractor](route: HttpRoute): String = {
    val hre = implicitly[HttpRouteExtractor[HttpRoute]]
    if (PREFER_ABS_URLS)
      hre.absoluteUrl( route, PREFER_SECURE_URLS )
    else
      hre.url( route )
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


  implicit class XhrFutExtOps(val xhrFut: Future[XMLHttpRequest]) extends AnyVal {

    /** Перехват и подмена стандартной AjaxException на более логгируемый вариант. */
    def catchAjaxEx(method: String = null, url: String = null): Future[XMLHttpRequest] = {
      xhrFut.recoverWith { case ex: AjaxException =>
        // Плохое логгирование исправляем сразу:
        val ex2 = XhrFailedException(
          xhr       = ex.xhr,
          url       = url,
          method    = method,
          getCause  = ex
        )
        Future.failed(ex2)
      }
    }

  }

}
