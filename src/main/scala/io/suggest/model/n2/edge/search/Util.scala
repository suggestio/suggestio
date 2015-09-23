package io.suggest.model.n2.edge.search

import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 13:58
 * Description: Утиль для написания поисковых аддонов [[io.suggest.model.n2.edge.MEdge]].
 */
object Util {
  
  def strSeqToQueryOpt(fn: String, values: Seq[String], qbOpt0: Option[QueryBuilder]): Option[QueryBuilder] = {
    if (values.isEmpty) {
      qbOpt0
    } else {
      qbOpt0.map[QueryBuilder] { qb0 =>
        // Фильтрануть по полю fromId.
        val mf = FilterBuilders.termsFilter(fn, values : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb0, mf)
      }
        .orElse {
        val qb = QueryBuilders.termsQuery(fn, values : _*)
        Some(qb)
      }
    }
  }

  def sbInitSz(sz0: Int, fnLen: Int, values: Seq[String]): Int = {
    if (values.isEmpty) {
      sz0
    } else {
      values.foldLeft(sz0 + fnLen + 8) { (acc, e) =>
        acc + 2 + e.length
      }
    }
  }

}
