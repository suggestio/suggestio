package io.suggest.sc.c.grid

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.{MSc3Resp, MScQs}
import io.suggest.sc.u.ScJsRoutesUtil

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
  def findAds(args: MScQs): Future[MSc3Resp]

  /** Запрос к серверу на тему фокусировки для карточки.
    *
    * @param args Аргументы поиска и фокусировки.
    * @return Фьючерс с распарсенным ответом сервера.
    *         Сервер может вернуть Index-ответ, это значит, что надо перейти в выдачу другого узла.
    */
  def focusedAds(args: MScQs): Future[MSc3Resp]

}


/** Реализация [[IFindAdsApi]] поверх HTTP/XHR. */
trait FindAdsApiXhrImpl extends IFindAdsApi {

  override def findAds(args: MScQs): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args  = args,
      route = ScJsRoutes.controllers.Sc.findAds
    )
  }

  override def focusedAds(args: MScQs): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args  = args,
      route = ScJsRoutes.controllers.Sc.focusedAds
    )
  }

}
