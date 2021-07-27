package io.suggest.proto.http.client

import io.suggest.common.empty.OptionUtil.BoolOptOps

import java.net.URI
import io.suggest.common.html.HtmlConstants
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.adp.HttpClientAdp
import io.suggest.proto.http.client.adp.fetch.FetchAdp
import io.suggest.proto.http.client.adp.xhr.XhrAdp
import io.suggest.proto.http.client.cache.HttpCaching
import io.suggest.proto.http.cookie.{HttpCookieUtil, MCookieMeta, MCookieState}
import io.suggest.proto.http.model.{HttpReq, HttpReqAdp, HttpResp, IHttpResultHolder}
import io.suggest.routes.HttpRouteExtractor
import io.suggest.sec.SessionConst
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.text.{StringUtil, UrlUtil2}
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 11:14
  * Description: HTTP-клиент.
  */
object HttpClient extends Log {

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
    val adp = if ((httpReq.data.forceXhr || httpReq.data.onProgress.nonEmpty) && XhrAdp.isAvailable) {
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

    lazy val reqUri = new URI( httpReq.url )

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
        // Отфильтровать кукис по домену, чтобы ошибочно не отправить сессию куда-то не по адресу.
        // Поддомены - игнорируем: домен должен точно совпадать.
        if {
          val cookieDomainOpt = sessionCookie.parsed.domain
            .orElse {
              // Определить исходный домен из ссылки в js-роутере
              httpReq.data.config.cookieDomainDflt.map(_())
            }
          var reqDomainOpt = Option( reqUri.getHost )  // getHost returns null for file:/// URLs.
          val r = (for {
            reqDomain <- reqDomainOpt
            cookieDomain <- cookieDomainOpt
          } yield {
            reqDomain equalsIgnoreCase cookieDomain
          })
            .getOrElseFalse

          if (!r) logger.warn( ErrorMsgs.COOKIE_DOMAIN_UNEXPECTED, msg = (reqDomainOpt, cookieDomainOpt, httpReq.method, httpReq.url) )
          r
        }
      } {
        val cookieValue = sessionCookie.parsed.toCookie
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
            (for {
              oneCookieV <- setCookies.iterator
              cookiesParsed <- {
                val eith = HttpCookieUtil.parseCookies( oneCookieV )

                for (err <- eith.left)
                  logger.error( ErrorMsgs.COOKIE_NOT_PARSED, msg = (err, if (scalajs.LinkingInfo.developmentMode) oneCookieV else StringUtil.strLimitLen(oneCookieV, 30)) )

                eith
                  .toOption
                  .iterator
              }
              // Может быть несколько кукисов в одной строке. Fetch/cdv-fetch так и возвращают значения одноимённых хидеров одной строкой.
              cookieParsed <- cookiesParsed.iterator
              // Финальные проверки присланного распарсенного кукиса:
              if {
                // Фильтрация по имени кукиса. Если в будущем тут будет более одного кукиса, то надо nextOption() заменить ниже.
                (cookieParsed.name ==* SessionConst.SESSION_COOKIE_NAME) &&
                // Фильтрануть по домену, что присланный домен соответствовал хосту из ссылки:
                cookieParsed.domain.fold(true) { cookieDomain =>
                  val reqDomain = reqUri.getHost
                  val r = reqDomain equalsIgnoreCase cookieDomain
                  if (!r) logger.error( ErrorMsgs.COOKIE_DOMAIN_UNEXPECTED, msg = (cookieParsed.name, cookieDomain, reqDomain) )
                  r
                }
              }
            } yield {
              MCookieState(
                parsed  = cookieParsed,
                meta    = MCookieMeta(),
              )
            })
              // Не более одного сессионного кукиса:
              .nextOption()
              .foreach( sessionTokenSetF )

            // вернуть исходный resp:
            httpResp
          }
        }
      }
  }

}
