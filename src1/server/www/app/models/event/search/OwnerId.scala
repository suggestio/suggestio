package models.event.search

import io.suggest.model.search.DynSearchArgs
import org.elasticsearch.index.query.{QueryBuilders, QueryBuilder}
import models.event.MEvent.OWNER_ID_ESFN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.15 16:42
 * Description: MEvent поиск: аддон для фильрации по полю ownerId.
 */
trait OwnerId extends DynSearchArgs {

  /** Искать-фильтровать по значению поля ownerId. */
  def ownerId: Option[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qOpt0 = super.toEsQueryOpt
    ownerId.fold(qOpt0) { _ownerId =>
      val fq = QueryBuilders.termQuery(OWNER_ID_ESFN, _ownerId)
      qOpt0.map { q0 =>
        QueryBuilders.boolQuery()
          .must(q0)
          .filter(fq)
      }.orElse {
        Some(fq)
      }
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("ownerId", ownerId, super.toStringBuilder)
  }

}
