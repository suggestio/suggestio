package io.suggest.routes

import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.proto.HttpConst.Methods
import io.suggest.sjs.common.model.{HttpRoute, HttpRouteExtractor}
import io.suggest.sjs.common.xhr.Xhr
import play.api.libs.json.Json
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

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


object IAdvRcvrsMapApi {

  def _advRcvrsMapRequest[HttpRoute: HttpRouteExtractor](route: HttpRoute): Future[MGeoNodesResp] = {
    for {
      jsonStr <- Xhr.requestJsonText( route )
    } yield {
      MGeoNodesResp(
        nodes = Json
          .parse(jsonStr)
          .as[Seq[MGeoNodePropsShapes]]
      )
    }
    //Xhr.unBooPickleResp[MGeoNodesResp] {
    //  Xhr.requestBinary( route )
    //}
  }

}

/** Реализация [[IAdvRcvrsMapApi]] поверх HTTP через js-роутер. */
class AdvRcvrsMapApiHttpViaRouter(jsRouter: => IJsRouter) extends IAdvRcvrsMapApi {

  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  override def advRcvrsMap(): Future[MGeoNodesResp] = {
    IAdvRcvrsMapApi._advRcvrsMapRequest(
      jsRouter.controllers.Static.advRcvrsMap()
    )
  }

}


/** Реализация [[IAdvRcvrsMapApi]] с запросом через произвольную ссылку. */
class AdvRcvrsMapApiHttpViaUrl(url: => String) extends IAdvRcvrsMapApi {

  override def advRcvrsMap(): Future[MGeoNodesResp] = {
    IAdvRcvrsMapApi._advRcvrsMapRequest(
      HttpRoute(method = Methods.GET, url = url)
    )
  }

}

