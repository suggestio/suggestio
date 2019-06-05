package io.suggest.proto.http.client

import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.adp.HttpClientAdp
import io.suggest.proto.http.client.adp.fetch.FetchAdp
import io.suggest.proto.http.client.adp.xhr.XhrAdp
import io.suggest.proto.http.model.HttpRouteExtractor
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.text.UrlUtil2
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 11:14
  * Description: HTTP-клиент, .
  */
object HttpClient extends Log {

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
  val mkAbsUrlIfPreferred: String => String = if (HttpClient.PREFER_ABS_URLS) {
    // Фунция на случай, когда требуется причёсывать ссылки:
    val httpProto = HttpConst.Proto.HTTP
    val isSecure = true

    url0: String =>
      UrlUtil2.mkAbsUrl( protoPrefix = httpProto, secure = isSecure, relUrl = url0 )

  } else {
    // Причёсывать ссылки не требуется. Просто используем исходные ссылки.
    identity[String]
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
  val execute: HttpClientAdp = {
    Stream.cons[HttpClientAdp](
      FetchAdp,
      XhrAdp #:: Stream.empty[HttpClientAdp]
    )
      .find(_.isAvailable)
      .get
  }

}


