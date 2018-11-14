package io.suggest.maps.u

import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.proto.HttpConst
import io.suggest.routes.{IJsRouter, routes}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.model.{HttpRoute, HttpRouteExtractor}
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
  def advRcvrsMapJson(): Future[MGeoNodesResp]

}


object IAdvRcvrsMapApi {

  def _advRcvrsMapRequest[HttpRoute: HttpRouteExtractor](route: HttpRoute): Future[MGeoNodesResp] = {
    for {
      list <- Xhr.unJsonResp[List[MGeoNodePropsShapes]] {
        Xhr.requestJsonText( route )
      }
    } yield {
      MGeoNodesResp( list )
    }
  }


  /** Сборка роуты на json-карту ресиверов.
    *
    * @param rcvrsMapHashSum Ключ для карты.
    * @param cdnHost Хост для связи, обычно CDN-хост.
    * @param jsRoutes Используемый js-роутер.
    * @return Абсолютная ссылка на ресурс.
    */
  def rcvrsMapRouteFromArgs(args: MRcvrsMapUrlArgs, jsRoutes: IJsRouter = routes): HttpRoute = {
    // Подготовить относительную ссылку:
    val route = jsRoutes.controllers.Static.advRcvrsMapJson( args.hashSum )
    // прикрутить CDN-host
    val hostedUrl = HttpConst.Proto.CURR_PROTO + args.cdnHost + route.url
    // Дописать протокол для связи с сервером, если у нас тут приложение или иные особые условия:
    HttpRoute(
      method = route.method,
      url    = Xhr.mkAbsUrlIfPreferred( hostedUrl )
    )
  }

}


/** Реализация [[IAdvRcvrsMapApi]] с запросом через произвольную ссылку. */
class AdvRcvrsMapApiHttpViaUrl[Route_t: HttpRouteExtractor](route: () => Route_t) extends IAdvRcvrsMapApi {

  override def advRcvrsMapJson(): Future[MGeoNodesResp] = {
    IAdvRcvrsMapApi._advRcvrsMapRequest( route() )
  }

}

