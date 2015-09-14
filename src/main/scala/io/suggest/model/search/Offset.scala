package io.suggest.model.search

import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 17:38
 * Description: Поддержка поля offset для сдвига в возвращаемых результатах запроса.
 */
trait Offset extends DynSearchArgs {

  /** Абсолютный сдвиг в возвращаемых результатах поиска. */
  def offset: Int

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    srb1.setFrom(Math.max(0, offset))
  }

}


/** Выставить дефолтовое значение оффсета.  */
trait OffsetDflt extends Offset {
  override def offset: Int = 0
}


/** Враппинг значения другой реализации оффсета. */
trait OffsetWrap extends Offset with DynSearchArgsWrapper {
  override type WT <: Offset

  override def offset = _dsArgsUnderlying.offset
}