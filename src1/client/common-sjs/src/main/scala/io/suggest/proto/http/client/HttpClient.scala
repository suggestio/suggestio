package io.suggest.proto.http.client

import io.suggest.common.html.HtmlConstants
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.adp.HttpClientAdp
import io.suggest.proto.http.client.adp.fetch.FetchAdp
import io.suggest.proto.http.client.adp.xhr.XhrAdp
import io.suggest.proto.http.client.cache.HttpCaching
import io.suggest.proto.http.model.{HttpReq, HttpReqAdp, HttpReqData, HttpResp, HttpRespTypes, IHttpResultHolder, MHttpCookie}
import io.suggest.routes.HttpRouteExtractor
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.text.UrlUtil2
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 11:14
  * Description: HTTP-клиент.
  */
object HttpClient {

  private def myProto: Option[String] = {
    for {
      location <- Option( dom.window.location )
      if !js.isUndefined( location )
      proto <- Option( location.protocol )
      if !js.isUndefined(proto) &&
         proto.nonEmpty &&
         proto !=* String.valueOf( null.asInstanceOf[Object] )
    } yield {
      proto.toLowerCase
    }
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

  def mkAbsUrl(url: String): String = {
    UrlUtil2.mkAbsUrl(
      protoPrefix = HttpConst.Proto.HTTP,
      secure = PREFER_SECURE_URLS,
      relUrl = url,
    )
  }

  /** Функция доп.обработки URL для допиливания их до ранга абсолютных, когда это необходимо.
    * Бывает, что требуются строго абсолютные URL (cordova). Тут - собираем фунцкию для причёсывания исходных ссылок.
    */
  val mkAbsUrlIfPreferred: String => String = {
    if (HttpClient.PREFER_ABS_URLS) {
      // Фунция на случай, когда требуется причёсывать ссылки:
      mkAbsUrl
    } else {
      // Причёсывать ссылки не требуется. Просто используем исходные ссылки.
      identity[String]
    }
  }


  /** Флаг предпочтения https над http при сборки абсолютных ссылок. */
  lazy val PREFER_SECURE_URLS: Boolean = {
    myHttpProto.fold(true) { proto =>
      // Обычно протокол описан как "http:" или "https:". Поэтому просто проверяем наличие буквы s в строке.
      proto contains HttpConst.Proto.SECURE_SUFFIX
    }
    //println("Xhr.secure = " + r)    // Нельзя тут LOG, иначе будет StackOverflowError во время инициализации RME-логгера.
  }


  def route2url[HttpRoute: HttpRouteExtractor](route: HttpRoute): String = {
    val hre = implicitly[HttpRouteExtractor[HttpRoute]]
    if (PREFER_ABS_URLS)
      hre.absoluteUrl( route, PREFER_SECURE_URLS )
    else
      hre.url( route )
  }


  /** Вернуть реальное API для отправки http-запросов. */
  val executor: HttpClientAdp = {
    // FetchAdp приоритетен и нужен для очень нужного Cache API (кэширования в моб.приложении и не только).
    ( FetchAdp #::
      XhrAdp #::
      LazyList.empty[HttpClientAdp]
    )
      .find(_.isAvailable)
      .get
  }


  /** Запуск HTTP-запроса на исполнение.
    *
    * @param httpReq Описание HTTP-реквеста и сопутствующих данных.
    * @return resp holder.
    */
  def execute(httpReq: HttpReq): IHttpResultHolder[HttpResp] = {
    // Выбрать адаптер и запустить обработку.
    val adp = if (httpReq.data.onProgress.nonEmpty && XhrAdp.isAvailable) {
      XhrAdp
    } else if (httpReq.data.config.fetchApi.nonEmpty) {
      // Явно задана fetch-функция в данных реквеста -- отправляем запрос через Fetch-адаптер:
      FetchAdp
    } else {
      executor
    }

    // Если в конфиге задан CSRF-токен, то добавить его в ссылку:
    val urlOpt2 = for {
      csrfToken <- httpReq.data.config.csrfToken
    } yield {
      val amp = "&"
      val qMark = HtmlConstants.QUESTION_MARK
      val equal = "="
      val url0 = httpReq.url
      if (s"[$amp$qMark]${csrfToken.qsKey}$equal".r.pattern.matcher(url0).find()) {
        // Гипотетически возможна ситуация, что в ссылке есть какой-то другой CSRF Token. Надо её отрабатывать?
        url0
      } else {
        val delim = if (url0 contains qMark) amp else qMark
        s"$url0$delim${js.URIUtils.encodeURIComponent(csrfToken.qsKey)}$equal${js.URIUtils.encodeURIComponent(csrfToken.value)}"
      }
    }

    // Собрать все заголовки запроса воедино:
    val allHeaders = {
      var hdrs0 = httpReq.data.headers

      // Залить baseHeaders в заголовки запроса, но с приоритетом исходных заголовков:
      if (httpReq.data.config.baseHeaders.nonEmpty)
        hdrs0 = httpReq.data.config.baseHeaders ++ hdrs0

      // Если sessionToken доступен, то закинуть его в заголовки:
      for {
        sessionCookieGetF   <- httpReq.data.config.sessionCookieGet
        sessionCookie       <- sessionCookieGetF()
        // TODO Фильтровать кукис по домену и валидности.
      } {
        val cookieValue = sessionCookie.cookieHeaderValue
        hdrs0 += (HttpConst.Headers.COOKIE -> cookieValue)
      }

      hdrs0
    }

    // Собрать данные будущего запроса:
    val adpInst = adp.factory(
      HttpReqAdp(
        origReq       = httpReq,
        reqUrlOpt     = urlOpt2,
        allReqHeaders = allHeaders,
      )
    )

    // Запустить HTTP-запрос с учётом настроек кэширования:
    val respHolder = HttpCaching.processCaching( adpInst )

    // Если задан sessionTokenSet, то распарсить...
    httpReq.data.config
      .sessionCookieSet
      .fold [IHttpResultHolder[HttpResp]] (respHolder) { sessionTokenSetF =>
        respHolder.mapResult { httpRespFut =>
          for (httpResp <- httpRespFut) yield {
            val setCookies = httpResp.getHeader( HttpConst.Headers.SET_COOKIE )
            for {
              oneCookieV <- setCookies.iterator
              // Довольно убогое решение: фильтровать только длинный кукис. TODO Лучше префикс проверять: s=
              if oneCookieV.length > 42
              oneCookie = MHttpCookie( oneCookieV )
            } {
              // TODO XXX нужна поддержка cookie discard, когда присылаемый сервером Expires/Max-Age невалиден или value пуст.
              val optValue = Some( oneCookie )
              sessionTokenSetF( optValue )
            }
            // вернуть исходный resp:
            httpResp
          }
        }
      }
  }


  /** Метод быстрой выкачки указанной ссылки.
    * Применяется для конвертации из base64-URL или Blob-URL в dom.Blob.
    *
    * @param url base64: или blob: URL, хотя можно любой.
    * @return Фьючерс с блобом.
    *         Future.failed при ошибке, в том числе если Fetch API не поддерживается.
    */
  def getBlob(url: String): Future[dom.Blob] = {
    execute(
      new HttpReq(
        method  = HttpConst.Methods.GET,
        url     = url,
        data    = HttpReqData(
          respType = HttpRespTypes.Blob,
        ),
      )
    )
      .resultFut
      .flatMap(_.blob())
  }

}
