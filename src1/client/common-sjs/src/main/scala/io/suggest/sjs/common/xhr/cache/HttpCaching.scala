package io.suggest.sjs.common.xhr.cache

import io.suggest.sjs.common.xhr.{FetchHttpResp, FetchRequestInit, HttpReq, HttpResp}
import org.scalajs.dom.experimental.Request
import japgolly.univeq._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 15:48
  * Description: Поддержка кэширования http-ответов на стороне js.
  * Кэш на стороне SW неудобен, т.к. не работает в cordova вообще и недостаточно контроллируется.
  * Т.к. caches API завязано на Fetch API, XHR-запросов это всё не касается.
  */
object HttpCaching {

  /** Высокоуровневое API для организации кэширования на стороне JS.
    *
    * @param req Исходный http-реквест.
    * @param request Скомпиленные данные будущего реквеста.
    * @param mkRequest Функция запуска реквеста на исполнение. Обычно просто вызов fetch().
    *                  Запускаемый реквест передаётся первым аргументом.
    * @return
    */
  def processCaching(req: HttpReq, requestInit: FetchRequestInit)
                    (mkRequest: (Request) => Future[FetchHttpResp]): Future[HttpResp] = {

    /** Запуск исходного реквеста. */
    lazy val __networkResp: Future[HttpResp] = {
      val request = new Request( req.url, requestInit.toRequestInit )
      mkRequest(request)
    }

    // Разобраться, надо ли кэшировать что-либо.
    req.data.cache.policy match {
      case MHttpCachingPolicies.NetworkOnly =>
        __networkResp

      case MHttpCachingPolicies.NetworkFirst =>
        __networkResp
        // TODO .recover(cache.get()), etc
        ???

      case MHttpCachingPolicies.Fastest =>
        __networkResp
        // TODO cache.get(), etc
        ???
    }

    ???
  }

}
