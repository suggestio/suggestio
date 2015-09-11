package io.suggest.model.n2.tag.edge.search

import io.suggest.model.n2.tag.edge.MTagEdge.ID_ESFN
import io.suggest.ym.model.common.{DynSearchArgs, DynSearchArgsWrapper}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.15 11:27
 * Description: Поиск по всем перечисленным тегам.
 */
trait WithAllTags extends DynSearchArgs {

  /** Искать документы, имеющие одновременно все перечисленные теги. */
  def withAllTags: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _wat = withAllTags
    if (_wat.isEmpty) {
      qbOpt0
    } else {
      qbOpt0.map { qb =>
        val fb = FilterBuilders.termsFilter(ID_ESFN, _wat: _*)
          .execution("and")
        QueryBuilders.filteredQuery(qb, fb)
      }
      .orElse {
        val qb2 = QueryBuilders.termsQuery(ID_ESFN, _wat: _*)
          .minimumMatch(_wat.size)
        Some(qb2)
      }
    }
  }

}


/** Дефолтовая реализация [[WithAllTags]]. */
trait WithAllTagsDflt extends WithAllTags {
  override def withAllTags: Seq[String] = Nil
}


/** Wrap-реализация [[WithAllTags]]. */
trait WithAllTagsWrapper extends WithAllTags with DynSearchArgsWrapper {
  override type WT <: WithAllTags
  override def withAllTags = _dsArgsUnderlying.withAllTags
}
