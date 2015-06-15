package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.sjs.m.msrv.MSrvUtil
import io.suggest.sc.sjs.util.router.srv.routes

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:17
 * Description: Модель асинхронного поиска focused-карточек через focused-APIv2.
 */
object MFocAds {

  /**
   * Отправить на сервер запрос поиска карточек.
   * @param args Аргументы.
   * @return Фьючерс с распарсенным ответом.
   */
  def find(args: MFocAdSearch)(implicit ec: ExecutionContext): Future[MFocAds] = {
    val route = routes.controllers.MarketShowcase.focusedAds(args.toJson)
    MSrvUtil.reqJson(route) map { json =>
      new MFocAds( MRespJson(json) )
    }
  }

}


/** Реализация модели ответов на запросы к focused-api. */
class MFocAds(json: MRespJson) {

  lazy val focusedAds: Seq[MFocAd] = {
    json.fads
      .iterator
      .map { MFocAd(_) }
      .toSeq
  }

}
