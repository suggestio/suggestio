package io.suggest.model.n2.media.search

import io.suggest.es.search._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 22:38
  * Description: Модель параметров поиска в модели MMedia.
  */
trait MMediaSearch
  extends SubSearches
  with FileHashSearch
  with NodeIdSearch
  with FileMimeSearch
  with FileSizeSearch
  with IsOriginalFileSearch
  with Limit
  with Offset

/** Класс, реализующий MMediaSearch (для снижения кодогенерации в реализациях) */
abstract class MMediaSearchImpl
  extends MMediaSearch


/** Дефолт для [[MMediaSearch]]. */
trait MMediaSearchDflt
  extends MMediaSearch
  with SubSearchesDflt
  with FileHashSearchDflt
  with NodeIdSearchDflt
  with FileMimeSearchDflt
  with FileSizeSearchDflt
  with IsOriginalFileSearchDflt
  with LimitDflt
  with OffsetDflt

/** Класс, реализующий [[MMediaSearchDflt]] (для снижения кодо-генерации в реализациях). */
class MMediaSearchDfltImpl
  extends MMediaSearchImpl
  with MMediaSearchDflt

