package models.event.search

import io.suggest.ym.model.common.DynSearchArgs
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}
import models.event.MEvent.IS_UNSEEN_ESFN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.15 16:44
 * Description: MEvent search: аддон для поддержки поиска/фильтрации по выставленному флагу isUnseen.
 */
trait OnlyUnseen extends DynSearchArgs {

  /** Искать/фильтровать по выставленному значению флага IS_UNSEEN. */
  def onlyUnseen: Boolean

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt
      // Отрабатываем isUnseen фильтром или запросом.
      .map { qb =>
        if (onlyUnseen) {
          val filter = FilterBuilders.termFilter(IS_UNSEEN_ESFN, true)
          QueryBuilders.filteredQuery(qb, filter)
        } else {
          qb
        }
      }
      .orElse {
        if (onlyUnseen) {
          val qb = QueryBuilders.termQuery(IS_UNSEEN_ESFN, true)
          Some(qb)
        } else {
          None
        }
      }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    val sb = super.toStringBuilder
    if (onlyUnseen)
      fmtColl2sb("isUnseen", Seq(true), sb)
    sb
  }
}
