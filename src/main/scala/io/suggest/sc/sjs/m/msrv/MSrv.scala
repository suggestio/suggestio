package io.suggest.sc.sjs.m.msrv

import cordova.Cordova
import io.suggest.sc.ScConstants.Vsns
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 10:19
 * Description: Статическая модель для переменных и констант работы с сервером.
 */
object MSrv {

  def IS_PLAIN_BROWSER = js.isUndefined( Cordova )

  /** Версия API backend-сервера. Записывается в запросы к sio-серверу, везде где это возможно. */
  val API_VSN: Int = {
    if ( IS_PLAIN_BROWSER ) {
      Vsns.SITE_JSONHTML
    } else {
      Vsns.CORDOVA_JSONHTML
    }
  }


  /** Выполнить реквест на север по указанной роуте.
    *
    * Дедубликация кода различных статических моделей, делающих запросы к серверу
    * с ответами по новому JSON-протоколу.
    *
    * @param route Роута для запроса.
    * @return Фьючерс с [[MScResp]] внутри.
    *         Future.failed, если запрос не удался.
    */
  def doRequest(route: Route): Future[MScResp] = {
    for {
      json <- Xhr.requestJson(route)
    } yield {
      MScResp(json)
    }
  }

}
