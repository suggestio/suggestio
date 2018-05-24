package io.suggest.sc.c.search

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.{MSc3Resp, MScQs}
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
  def tagsSearch(args: MScQs): Future[MSc3Resp]

}


/** Реализация [[ISearchApi]] поверх HTTP XHR. */
trait SearchApiXhrImpl extends ISearchApi {

  override def tagsSearch(args: MScQs): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args,
      route = ScJsRoutes.controllers.Sc.tagsSearch
    )
  }

}
