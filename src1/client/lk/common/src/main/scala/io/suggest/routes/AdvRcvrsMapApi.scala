package io.suggest.routes

import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.17 16:27
  * Description: js-роуты для Static-контроллера.
  */

/** Интерфейс для статического API. */
trait IAdvRcvrsMapApi {

  /** Получение десериализованного инстанса с данными гео.карты рекламщиков. */
  def advRcvrsMap(): Future[MGeoNodesResp]

}


/** Реализация [[IAdvRcvrsMapApi]] поверх HTTP через js-роутер. */
class AdvRcvrsMapApiHttp(jsRouter: => IJsRouter) extends IAdvRcvrsMapApi {

  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  override def advRcvrsMap(): Future[MGeoNodesResp] = {
    val route = jsRouter.controllers.Static.advRcvrsMap()
    // Надо запустить запрос на сервер для получения списка узлов.
    Xhr.unBooPickleResp[MGeoNodesResp] {
      Xhr.requestBinary( route )
    }
  }

}
