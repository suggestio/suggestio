package io.suggest.sc.grid.c

import io.suggest.routes.routes
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.sc3.MSc3Resp
import io.suggest.routes.JsRoutes_ScControllers._
import io.suggest.sc.router.c.ScJsRoutesUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:34
  * Description: API для поиска рекламных карточек для плитки.
  */
trait IFindAdsApi {

  /** Запуск поиска рекламных карточек.
    *
    * @param args Аргументы для поиска.
    * @return Фьючерс в обычном формате выдачи.
    */
  def findAds(args: MFindAdsReq): Future[MSc3Resp]

}


/** Реализация [[IFindAdsApi]] поверх HTTP/XHR. */
trait FindAdsXhrImpl extends IFindAdsApi {

  override def findAds(args: MFindAdsReq): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args = args,
      route = routes.controllers.Sc.findAds
    )
  }

}
