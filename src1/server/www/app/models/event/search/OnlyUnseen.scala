package models.event.search

import io.suggest.es.search.DynSearchArgs
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
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
    val fq = QueryBuilders.termQuery(IS_UNSEEN_ESFN, true)
    super.toEsQueryOpt
      // Отрабатываем isUnseen фильтром или запросом.
      .map { qb =>
        if (onlyUnseen) {
          QueryBuilders.boolQuery()
            .must(qb)
            .filter(fq)
        } else {
          qb
        }
      }
      .orElse {
        if (onlyUnseen) {
          Some(fq)
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
