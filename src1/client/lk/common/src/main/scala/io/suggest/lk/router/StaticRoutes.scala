package io.suggest.lk.router

import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.17 16:27
  * Description: js-роуты для Static-контроллера.
  */
@js.native
trait StaticRoutes extends js.Object {

  /** Роута для доступа к данным гео.карты рекламщиков.
    * Обычно проходит через CDN, но это уже разруливает серверный js-роутер. */
  def advRcvrsMap(): Route = js.native

}


trait IStaticApi {

  /** Получение десериализованного инстанса с данными гео.карты рекламщиков. */
  def advRcvrsMap(): Future[MGeoNodesResp]

}


/** Реализация [[IStaticApi]] поверх HTTP через js-роутер. */
class StaticHttpApi extends IStaticApi {

  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  override def advRcvrsMap(): Future[MGeoNodesResp] = {
    // Надо запустить запрос на сервер для получения списка узлов.
    val route = jsRoutes.controllers.Static.advRcvrsMap()
    Xhr.unBooPickleResp[MGeoNodesResp] {
      Xhr.requestBinary(route)
    }
  }

}
