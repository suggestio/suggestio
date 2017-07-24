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

/** Интерфейс для статического API. */
trait IStaticApi {

  /** Получение десериализованного инстанса с данными гео.карты рекламщиков. */
  def advRcvrsMap(): Future[MGeoNodesResp]

}


/** Реализация [[IStaticApi]] поверх HTTP XHR, но без конкретной js-роуты. */
trait AbstractStaticHttpApi extends IStaticApi {

  /** http-роута для получения гео-данных по ресиверам. */
  def advRcvrsMapRoute: Route

  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  override def advRcvrsMap(): Future[MGeoNodesResp] = {
    // Надо запустить запрос на сервер для получения списка узлов.
    Xhr.unBooPickleResp[MGeoNodesResp] {
      Xhr.requestBinary( advRcvrsMapRoute )
    }
  }

}


/** Реализация [[IStaticApi]] поверх HTTP через js-роутер. */
class StaticHttpApi extends AbstractStaticHttpApi {

  override def advRcvrsMapRoute = jsRoutes.controllers.Static.advRcvrsMap()

}
