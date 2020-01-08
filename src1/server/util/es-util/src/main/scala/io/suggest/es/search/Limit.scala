package io.suggest.es.search

import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 17:29
 * Description:
 */

/** Интерфейс для поля limit: Int. */
trait ILimit {

  /** Макс.кол-во результатов. */
  def limit: Int
}


/** DynSearch-аддон для ограничевания количества возвращаемых результатов поиска. */
trait Limit extends DynSearchArgs with ILimit {

  /** Жесткое ограничение сверху по кол-ву результатов поиска. По идее, оно не должно влиять на выдачу никогда.
    * Нужно для защиты от ddos при недостаточной проверке значения maxResults на верхнем уровне. */
  def MAX_RESULTS_HARD = 1000

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    if (limit >= 0) {
      val lim = Math.min(MAX_RESULTS_HARD, limit)
      srb1.setSize(lim)
    } else {
      srb1
    }
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("limit", Seq(limit).filter(_ > 0), super.toStringBuilder)
  }
  override def sbInitSize: Int = super.sbInitSize + 16

}


trait LimitDflt extends Limit {
  override def limit: Int = 10
}
