package io.suggest.proto.http.model

import io.suggest.i18n.MLanguage
import io.suggest.proto.http.cookie.MCookieState
import japgolly.univeq._
import monocle.macros.GenLens
import org.scalajs.dom.experimental.{RequestInfo, RequestInit}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.08.2020 9:42
  * Description: Модель необязательного конфига для запросов http-клиентов.
  */
object HttpClientConfig {

  def empty = apply()

  @inline implicit def univEq: UnivEq[HttpClientConfig] = UnivEq.force

  def csrfToken = GenLens[HttpClientConfig]( _.csrfToken )

}


/** Контейнер доп.конфигурации, падающей откуда-то с верхних уровней.
  *
  * @param csrfToken Данные CSRF для сборки ссылки.
  * @param baseHeaders Доп.заголовки http.
  * @param fetchApi Переопределение fetch()-функции, вместо стандартной.
  *                 cordova-plugin-fetch содержит более эффективную альтернативу webkit/blink-реализации fetch(),
  *                 которая режет заголовки ответа, местами недореализована.
  * @param forcePostBodyNonEmpty Для POST-запросов fetch через okhttp без тела может вылетать ошибка вида:
  *                              "method POST must have request body".
  *                              Выставление этого флага заставляет передавать пустое тело запроса.
  * @param language Cordova-only: Set suggest.io and/or other headers/cookies to requests to be in customized language.
  */
case class HttpClientConfig(
                             csrfToken          : Option[MCsrfToken]            = None,
                             baseHeaders        : Map[String, String]           = HttpReqData.mkBaseHeaders(),
                             cookies            : Option[IHttpCookies]          = None,
                             fetchApi           : Option[(RequestInfo, RequestInit) => Future[HttpResp]] = None,
                             forcePostBodyNonEmpty: Boolean                     = false,
                             language           : Option[MLanguage]             = None,
                           )

trait IMHttpClientConfig {
  // Доп.костыли для csrf не требуются. CSRF уже вставлен в js-роутер.
  def httpClientConfig: () => HttpClientConfig = () => HttpClientConfig.empty
}


/** Cookies state access. */
trait IHttpCookies {
  def sessionCookieGet(): Option[MCookieState]
  def sessionCookieSet(cookieState: MCookieState): Unit
  def cookieDomainDefault(): Option[String]
}