package io.suggest.model.n2.edge.search

import io.suggest.model.n2.node.MNode.Fields.Edges._
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import io.suggest.util.MacroLogsI
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:45
 * Description: Поиск по nested-документам в out-эджах.
 */
trait OutEdges extends DynSearchArgs with MacroLogsI {

  /** Поиск/фильтрация по out-эджам согласно описанным критериям. */
  def outEdges: Seq[ICriteria]

  /** Накатить эти сложные критерии на query. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val _outEdgesIter = outEdges
      .iterator
      .filter { _.nonEmpty }
    val qbOpt0 = super.toEsQueryOpt
    if (_outEdgesIter.isEmpty) {
      qbOpt0
    } else {
      val nq = QueryBuilders.boolQuery()
        .minimumNumberShouldMatch(1)
      // Сборка nested query.
      for (oe <- _outEdgesIter) {

        // Поиск по id узлов, на которые указывают эджи.
        var _qOpt: Option[QueryBuilder] = if (oe.nodeIds.nonEmpty) {
          val __q = QueryBuilders.termsQuery(EDGE_OUT_NODE_ID_FULL_FN, oe.nodeIds: _*)
          Some(__q)
        } else {
          None
        }

        // Предикаты рёбер добавить через фильтр либо query.
        if (oe.predicates.nonEmpty) {
          val fn = EDGE_OUT_PREDICATE_FULL_FN
          _qOpt = _qOpt map { _q =>
            val predf = FilterBuilders.termsFilter(fn, oe.nodeIds : _*)
            QueryBuilders.filteredQuery(_q, predf)
          } orElse {
            val _q = QueryBuilders.termsQuery(fn, oe.nodeIds : _*)
            Some(_q)
          }
        }

        // ad search receivers: добавить show levels
        if (oe.sls.nonEmpty) {
          val fn = EDGE_OUT_INFO_SLS_FN
          val slsStr = oe.sls.map(_.name)
          _qOpt = _qOpt map { _q =>
            val slsf = FilterBuilders.termsFilter(fn, slsStr : _*)
            QueryBuilders.filteredQuery(_q, slsf)
          } orElse {
            val _q = QueryBuilders.termsQuery(fn, slsStr: _*)
            Some( _q )
          }
        }

        _qOpt.foreach { _q =>
          nq.should(_q)
        }
        if (_qOpt.isEmpty)
          LOGGER.warn("edge.NestedSearch: suppressed empty bool query for " + oe)
      }
      // Сборка основной query
      qbOpt0.map { qb0 =>
        val nf = FilterBuilders.nestedFilter(EDGES_OUT_FULL_FN, nq)
        QueryBuilders.filteredQuery(qb0, nf)
      }.orElse {
        val qb = QueryBuilders.nestedQuery(EDGES_OUT_FULL_FN, nq)
        Some(qb)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val s0 = super.sbInitSize
    s0 + outEdges.length * 80
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("outEdges", outEdges, super.toStringBuilder)
  }

}


/** Дефолтовая реализация [[OutEdges]]. */
trait OutEdgesDflt extends OutEdges {
  override def outEdges: Seq[ICriteria] = Nil
}


/** Враппер для аддона [[OutEdges]]. */
trait OutEdgesWrap extends OutEdges with DynSearchArgsWrapper {
  override type WT <: OutEdges
  override def outEdges = _dsArgsUnderlying.outEdges
}
