package io.suggest.sc.sjs.m.msrv.nodes.find

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSON

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
  def findNodes(args: MFindNodesArgs)(implicit ec: ExecutionContext): Future[MFindArgsResp] = {
    val route = routes.controllers.MarketShowcase.findNodes( args.toJson )
    val reqFut = Xhr.successWithStatus(200) {
      Xhr.send(
        method  = route.method,
        url     = route.url,
        accept  = Some(Xhr.MIME_JSON)
      )
    }
    reqFut map { xhr =>
      // zero-copy десериализация ответа
      val json = JSON.parse( xhr.responseText )
      //  .asInstanceOf[MFindAdsRespJson]
      //MFindAds(json)
      ???
    }
  }

}
