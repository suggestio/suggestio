package io.suggest.model.n2.media.search

import io.suggest.es.search.{SubSearches, SubSearchesDflt}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 22:38
  * Description: Модель трейта параметров поиска в модели MMedia.
  */
trait MMediaSearch
  extends SubSearches
  with FileHashSearch

/** Класс, реализующий MMediaSearch (для снижения кодогенерации в реализациях) */
abstract class MMediaSearchImpl
  extends MMediaSearch



/** Дефолт для [[MMediaSearch]]. */
trait MMediaSearchDflt
  extends MMediaSearch
  with SubSearchesDflt
  with FileHashSearchDflt

/** Класс, реализующий [[MMediaSearchDflt]] (для снижения кодо-генерации в реализациях). */
class MMediaSearchDfltImpl
  extends MMediaSearchImpl
  with MMediaSearchDflt

