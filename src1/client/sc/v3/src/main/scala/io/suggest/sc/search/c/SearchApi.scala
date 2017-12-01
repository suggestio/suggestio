package io.suggest.sc.search.c

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 22:29
  * Description: Серверное API для поисковых нужд.
  */
trait ISearchApi {

  // TODO Вынести наконец модель MScTagsSearchQs в [common].
  //def tagsSearch(args: MTagsSea)

}


/** Реализация [[ISearchApi]] поверх HTTP XHR. */
trait SearchApiXhrImpl extends ISearchApi {

  // TODO

}
