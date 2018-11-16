package io.suggest.maps.u

import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.proto.HttpConst
import io.suggest.routes.{IJsRouter, routes}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.model.HttpRoute
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
  def advRcvrsMapJson(args: MRcvrsMapUrlArgs): Future[MGeoNodesResp]

}


/** Реализация [[IAdvRcvrsMapApi]] с запросом через произвольную ссылку. */
class AdvRcvrsMapApiHttpViaUrl(jsRoutes: IJsRouter = routes) extends IAdvRcvrsMapApi {

  override def advRcvrsMapJson(args: MRcvrsMapUrlArgs): Future[MGeoNodesResp] = {
    val route = jsRoutes.controllers.Static.advRcvrsMapJson( args.hashSum )
    for {
      list <- Xhr.unJsonResp[List[MGeoNodePropsShapes]] {
        Xhr.requestJsonText( route )
      }
    } yield {
      MGeoNodesResp( list )
    }
  }

}

