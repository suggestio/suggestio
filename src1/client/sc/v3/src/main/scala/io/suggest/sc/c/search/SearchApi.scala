package io.suggest.sc.c.search

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.MSc3TagsResp
import io.suggest.sc.tags.MScTagsSearchQs
import io.suggest.sc.u.ScJsRoutesUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 22:29
  * Description: Серверное API для поисковых нужд.
  */
trait ISearchApi {

  /** Запуск поиска гео-тегов на сервере.
    *
    * @param args Аргументы для поиска тегов.
    * @return Фьючерс с ответом сервера.
    */
  def tagsSearch(args: MScTagsSearchQs): Future[MSc3TagsResp]

}


/** Реализация [[ISearchApi]] поверх HTTP XHR. */
trait SearchApiXhrImpl extends ISearchApi {

  override def tagsSearch(args: MScTagsSearchQs): Future[MSc3TagsResp] = {
    ScJsRoutesUtil.mkRequest[MScTagsSearchQs, MSc3TagsResp](
      args,
      route = ScJsRoutes.controllers.Sc.tagsSearch
    )
  }

}
