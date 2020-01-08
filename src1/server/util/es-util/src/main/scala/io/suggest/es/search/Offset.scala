package io.suggest.es.search

import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 17:38
 * Description: Поддержка поля offset для сдвига в возвращаемых результатах запроса.
 */
trait Offset extends DynSearchArgs {

  /** Абсолютный сдвиг в возвращаемых результатах поиска. */
  def offset: Int = 0

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    srb1.setFrom(Math.max(0, offset))
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("offset", Seq(offset), super.toStringBuilder)
  }
  override def sbInitSize: Int = super.sbInitSize + 16

}
