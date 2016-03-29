package io.suggest.sc.sjs.m.msrv.nodes.find

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 15:57
 * Description: Модель взаимодействия с сервером в области поиска узлов рекламной сети.
 */
object MFindNodes {

  /**
   * Отправить запрос поиска списка узлов.
   * @param args Аргументы запроса.
   * @return Фьючерс с распарсенным ответом сервера.
   */
  def findNodes(args: MFindNodesArgs)(implicit ec: ExecutionContext): Future[MFindNodesResp] = {
    val route = routes.controllers.MarketShowcase.findNodes( args.toJson )
    for (json1 <- Xhr.getJson(route)) yield {
      val json2 = MFindAdsRespJson(json1)
      MFindNodesResp(json2)
    }
  }

}


/** Распарсенный ответ сервера на запрос списка узлов. */
case class MFindNodesResp(json: MFindAdsRespJson) {

  /** HTML-верстка списка узлов. */
  def nodeListHtml = json.nodes

  /** Время начала генерации ответа по часам сервера. */
  def timestamp = json.timestamp.toLong

}
