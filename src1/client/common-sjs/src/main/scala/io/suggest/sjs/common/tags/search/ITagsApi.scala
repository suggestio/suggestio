package io.suggest.sjs.common.tags.search

import io.suggest.common.tags.search.MTagsFound
import io.suggest.common.tags.search.MTagsFound.pickler
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 14:53
  * Description: Api для связи с сервером на тему поиска тегов.
  */
trait ITagsApi {

  /** Метод запроса к серверу для поиска тегов. */
  def tagsSearch(args: MTagSearchArgs): Future[MTagsFound]

}


/** Реализация [[ITagsApi]] через XHR с бинарщиной в ответе. */
trait TagsHttpApiImpl extends ITagsApi {

  /** Функция-генератор роуты для поиска тегов на сервере. */
  protected def _tagsSearchRoute: js.Dictionary[js.Any] => Route

  override def tagsSearch(args: MTagSearchArgs): Future[MTagsFound] = {
    val route = _tagsSearchRoute( MTagSearchArgs.toJson(args) )
    Xhr.unBooPickleResp[MTagsFound] {
      Xhr.requestBinary(route)
    }
  }

}
