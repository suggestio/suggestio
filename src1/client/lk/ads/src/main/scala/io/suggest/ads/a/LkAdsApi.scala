package io.suggest.ads.a

import io.suggest.ads.MGetAdsResp
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.Xhr
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:56
  * Description: API для доступа к серверу за карточками.
  */
trait ILkAdsApi {

  def getAds(nodeKey: RcvrKey, offset: Int): Future[MGetAdsResp]

}


/** Реализация [[ILkAdsApi]] поверх традиционных http-запросов. */
class LkAdsApiHttp() extends ILkAdsApi {

  import LkAdsRoutes._

  override def getAds(nodeKey: RcvrKey, offset: Int): Future[MGetAdsResp] = {
    val route = routes.controllers.LkAds.getAds(
      rcvrKey = RcvrKey.rcvrKey2urlPath( nodeKey ),
      offset  = offset
    )
    for {
      jsonStr <- Xhr.requestJsonText(route)
    } yield {
      Json
        .parse(jsonStr)
        .as[MGetAdsResp]
    }
  }

}
