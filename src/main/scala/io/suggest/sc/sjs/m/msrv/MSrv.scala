package io.suggest.sc.sjs.m.msrv

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 10:19
 * Description: Статическая модель для переменных и констант работы с сервером.
 */
object MSrv {

  /** Версия API backend-сервера. Записывается в запросы к sio-серверу, везде где это возможно. */
  def API_VSN: Int = 2


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
      json <- Xhr.getJson(route)
    } yield {
      MScResp(json)
    }
  }

}
