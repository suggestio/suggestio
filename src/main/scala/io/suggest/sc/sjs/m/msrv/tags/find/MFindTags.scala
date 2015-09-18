package io.suggest.sc.sjs.m.msrv.tags.find

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:17
 * Description: Модель для поиска тегов на стороне сервера.
 */
object MFindTags {

  /**
   * Поиск тегов по указанным критериям.
   * @param args Модель аргументов поиска.
   * @return Фьючерс с результатом поиска.
   */
  def search(args: MftArgsT)(implicit ec: ExecutionContext): Future[MftResp] = {
    val route = routes.controllers.MarketShowcase.tagsSearch( args.toJson )
    for (jsonRaw <- Xhr.getJson(route)) yield {
      MftResp.fromJson(jsonRaw)
    }
  }

}
