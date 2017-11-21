package io.suggest.sc.foc.c

import io.suggest.routes.scRoutes
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.router.c.ScJsRoutesUtil
import io.suggest.sc.sc3.MSc3Resp

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.11.17 21:14
  * Description: API для фокусировки на конкретной карточке.
  */
trait IFocAdsApi {

  /** Запрос к серверу на тему фокусировки для карточки.
    *
    * @param args Аргументы поиска и фокусировки.
    * @return Фьючерс с распарсенным ответом сервера.
    *         Сервер может вернуть Index-ответ, это значит, что надо перейти в выдачу другого узла.
    */
  def focusedAds(args: MFindAdsReq): Future[MSc3Resp]

}


/** Реализация [[IFocAdsApi]] для HTTP поверх XHR, т.е. обычные XHR-запросы к серверу. */
trait FocAdsXhrApi extends IFocAdsApi {

  import io.suggest.routes.JsRoutes_ScControllers._

  override def focusedAds(args: MFindAdsReq): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args  = args,
      route = scRoutes.controllers.Sc.focusedAds
    )
  }

}
