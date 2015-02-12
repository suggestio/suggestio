package models.event.search

import io.suggest.ym.model.common.DynSearchArgs
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders, QueryBuilder}
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
    super.toEsQueryOpt
      // Отрабатываем ownerId фильтром или запросом.
      .map { qb =>
        ownerId.fold(qb) { _ownerId =>
          val filter = FilterBuilders.termFilter(OWNER_ID_ESFN, _ownerId)
          QueryBuilders.filteredQuery(qb, filter)
        }
      }
      .orElse {
        ownerId.map { _ownerId =>
          QueryBuilders.termQuery(OWNER_ID_ESFN, _ownerId)
        }
      }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("ownerId", ownerId, super.toStringBuilder)
  }

}
