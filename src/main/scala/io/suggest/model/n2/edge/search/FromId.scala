package io.suggest.model.n2.edge.search

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}
import io.suggest.model.n2.edge.MEdge.FROM_ID_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 12:49
 * Description: Аддоны для поддержки поиска в поле fromId.
 */
trait FromId extends DynSearchArgs {

  /** id n2-узла, ссылающегося искомыми ребрами графа на другие узлы. */
  def fromId: Option[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    fromId.fold(qbOpt0) { _fromId =>
      qbOpt0.map[QueryBuilder] { qb0 =>
        // Фильтрануть по полю fromId.
        val mf = FilterBuilders.termFilter(FROM_ID_FN, _fromId)
        QueryBuilders.filteredQuery(qb0, mf)
      }
      .orElse {
        val qb = QueryBuilders.termQuery(FROM_ID_FN, _fromId)
        Some(qb)
      }
    }
  }

  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    fromId.fold(sz0) { _fromId =>
      sz0 + 12 + _fromId.length
    }
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("fromId", fromId, super.toStringBuilder)
  }

}


/** Дефолтовая реализация полей поискового аддона [[FromId]]. */
trait FromIdDflt extends FromId {
  override def fromId: Option[String] = None
}


/** Враппер-реализация полей поискового аддона [[FromId]]. */
trait FromIdWrapper extends FromId with DynSearchArgsWrapper {
  override type WT <: FromId
  override def fromId = _dsArgsUnderlying.fromId
}
