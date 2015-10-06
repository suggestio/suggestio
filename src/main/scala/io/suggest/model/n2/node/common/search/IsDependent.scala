package io.suggest.model.n2.node.common.search

import io.suggest.model.n2.node.MNode
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 15:42
 * Description: Поисковый аддон для поиска/фильтрации по полю MNode.common.isDependent.
 */
trait IsDependent extends DynSearchArgs {

  /** Критерий для поиска/фильтрации. */
  def isDependent: Option[Boolean]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _isDepOpt = isDependent
    if (_isDepOpt.isEmpty) {
      qbOpt0

    } else {
      val fn = MNode.Fields.Common.IS_DEPENDENT_FN
      val _isDep = _isDepOpt.get
      qbOpt0 map { qb =>
        val idf = FilterBuilders.termFilter(fn, _isDep)
        QueryBuilders.filteredQuery(qb, idf)

      } orElse {
        val qb = QueryBuilders.termQuery(fn, _isDep)
        Some(qb)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    val _isDepOpt = isDependent
    if (_isDepOpt.isEmpty) {
      sz0
    } else {
      sz0 + 16
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("isDepend", isDependent, super.toStringBuilder)
  }

}


/** Дефолтовая реализация поискового аддона [[IsDependent]]. */
trait IsDependentDflt extends IsDependent {
  override def isDependent: Option[Boolean] = None
}


/** Wrap-реализация поискового аддона [[IsDependent]]. */
trait IsDependentWrap extends IsDependent with DynSearchArgsWrapper {
  override type WT <: IsDependent
  override def isDependent = _dsArgsUnderlying.isDependent
}
