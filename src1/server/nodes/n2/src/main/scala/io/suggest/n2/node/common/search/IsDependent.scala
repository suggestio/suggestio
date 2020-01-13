package io.suggest.n2.node.common.search

import io.suggest.es.search.DynSearchArgs
import io.suggest.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 15:42
 * Description: Поисковый аддон для поиска/фильтрации по полю MNode.common.isDependent.
 */
trait IsDependent extends DynSearchArgs {

  /** Критерий для поиска/фильтрации. */
  def isDependent: Option[Boolean] = None

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _isDepOpt = isDependent
    if (_isDepOpt.isEmpty) {
      qbOpt0

    } else {
      val fn = MNodeFields.Common.IS_DEPENDENT_FN
      val _isDep = _isDepOpt.get
      val idf = QueryBuilders.termQuery(fn, _isDep)
      qbOpt0.map { qb =>
        QueryBuilders.boolQuery()
          .must(qb)
          .filter(idf)

      }.orElse {
        Some(idf)
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
