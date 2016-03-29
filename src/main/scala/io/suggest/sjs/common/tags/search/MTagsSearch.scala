package io.suggest.sjs.common.tags.search

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:17
 * Description: Модель для поиска тегов на стороне сервера.
 */
object MTagsSearch {

  /**
   * Поиск тегов по указанным критериям.
   *
   * @param route Ссылка с аргументами поиска.
   * @return Фьючерс с результатом поиска.
   */
  def search(route: Route)(implicit ec: ExecutionContext): Future[MTagSearchResp] = {
    val fut = Xhr.getJson(route)

    for (res <- fut) yield {
      MTagSearchResp.fromJson(res)
    }
  }

}
