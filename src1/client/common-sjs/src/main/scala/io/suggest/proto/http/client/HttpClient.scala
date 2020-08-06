package io.suggest.proto.http.client

import io.suggest.common.html.HtmlConstants
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.adp.HttpClientAdp
import io.suggest.proto.http.client.adp.fetch.FetchAdp
import io.suggest.proto.http.client.adp.xhr.XhrAdp
import io.suggest.proto.http.model.{HttpReq, IHttpRespHolder}
import io.suggest.routes.HttpRouteExtractor
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.text.UrlUtil2
import org.scalajs.dom

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
  val defaultExecutor: HttpClientAdp = {
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
  def execute(httpReq: HttpReq): IHttpRespHolder = {
    val adp = if (httpReq.data.onProgress.nonEmpty && XhrAdp.isAvailable) {
      XhrAdp
    } else {
      defaultExecutor
    }

    // Если в конфиге задан CSRF-токен, то добавить его в ссылку.
    val httpReq2 = (for {
      hcConfig <- httpReq.data.config
      csrfToken <- hcConfig.csrfToken
    } yield {
      (HttpReq.url.modify { url0 =>
        val amp = "&"
        val qMark = HtmlConstants.QUESTION_MARK
        val equal = "="
        if (s"[$amp$qMark]${csrfToken.qsKey}$equal".r.pattern.matcher(url0).find()) {
          // Гипотетически возможна ситуация, что в ссылке есть какой-то другой CSRF Token. Надо её отрабатывать?
          url0
        } else {
          val delim = if (url0 contains qMark) amp else qMark
          s"$url0$delim${js.URIUtils.encodeURIComponent(csrfToken.qsKey)}$equal${js.URIUtils.encodeURIComponent(csrfToken.value)}"
        }
      })( httpReq )
    })
      .getOrElse( httpReq )

    adp( httpReq2 )
  }

}
