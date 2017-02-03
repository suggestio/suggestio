package io.suggest.es.search

import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 17:38
 */

/** Интерфейс для поля offset: Int. */
trait IOffset {

  /** Абсолютный сдвиг в возвращаемых результатах поиска. */
  def offset: Int
}


/** Поддержка поля offset для сдвига в возвращаемых результатах запроса. */
trait Offset extends DynSearchArgs with IOffset {

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    srb1.setFrom(Math.max(0, offset))
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("offset", Seq(offset), super.toStringBuilder)
  }
  override def sbInitSize: Int = super.sbInitSize + 16

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