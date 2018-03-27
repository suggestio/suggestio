package io.suggest.sc.sjs.m.msrv.tile

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sjs.m.msrv.MSrv
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.16 15:30
  * Description: Поиск карточек плитки на сервере.
  */
object MFindAdsTile {

  /**
   * Запуск поиска рекламных карточек на сервере.
   * @param adSearch Поисковые критерии, которые может понять jsRouter.
   * @return Фьючерс с результатом запроса.
   */
  def findAds(adSearch: MFindAdsReq): Future[MScRespAdsTile] = {
    val route = ScJsRoutes.controllers.Sc.findAds( adSearch.toJson )

    for {
      mResp <- MSrv.doRequest(route)
    } yield {
      mResp
        .actions
        .head
        .adsTile.get
    }
  }

}
