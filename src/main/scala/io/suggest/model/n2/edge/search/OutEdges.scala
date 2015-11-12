package io.suggest.model.n2.edge.search

import io.suggest.model.n2.node.MNodeFields.Edges._
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
      var shouldClauses = 0
      val nq = QueryBuilders.boolQuery()
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
          val predIds = oe.predicates.map(_.strId)
          _qOpt = _qOpt map { _q =>
            val predf = FilterBuilders.termsFilter(fn, predIds: _*)
            QueryBuilders.filteredQuery(_q, predf)
          } orElse {
            val _q = QueryBuilders.termsQuery(fn, predIds: _*)
            Some(_q)
          }
        }

        // ad search receivers: добавить show levels
        if (oe.anySl.nonEmpty) {
          if (_qOpt.nonEmpty && oe.sls.isEmpty) {
            // missing/existing filter можно навешивать только если уже есть тело nested query
            val fn = EDGE_OUT_INFO_SLS_FN
            val f = if (oe.anySl.get) {
              FilterBuilders.existsFilter(fn)
            } else {
              FilterBuilders.missingFilter(fn)
            }
            val _nq2 = QueryBuilders.filteredQuery(_qOpt.get, f)
            _qOpt = Some(_nq2)

          } else {
            val msg = if (_qOpt.isEmpty ) {
              // Нельзя навешивать any sl-фильтры без заданного предиката или id узла.
              "so at least one of [.predicates, .nodeIds] must be non-empty"
            } else {
              // Нельзя одновременно задавать sls и anySl критерии.
              "but .sls is non empty too. Define at once only one of, not both"
            }
            throw new IllegalArgumentException("outEdges Criteria: .anySl is defined, " + msg + ": " + oe)
          }

        } else if (oe.sls.nonEmpty) {
          val slsStr = oe.sls.map(_.name)
          val slFn = EDGE_OUT_INFO_SLS_FN
          _qOpt = _qOpt.map { _q =>
            val slsf = FilterBuilders.termsFilter(slFn, slsStr : _*)
            QueryBuilders.filteredQuery(_q, slsf)
          }.orElse {
            val _q = QueryBuilders.termsQuery(slFn, slsStr: _*)
            Some( _q )
          }
        }

        // Ищем/фильтруем по info.flag
        if (oe.flag.nonEmpty) {
          val flag = oe.flag.get
          val flagFn = EDGE_OUT_INFO_FLAG_FN
          _qOpt = _qOpt.map { _q =>
            val flagFl = FilterBuilders.termFilter(flagFn, flag)
            QueryBuilders.filteredQuery(_q, flagFl)
          }.orElse {
            val _q = QueryBuilders.termQuery(flagFn, flag)
            Some(_q)
          }
        }

        // Возврат значения происходит через закидывание сгенеренной query в BoolQuery.
        for (_q <- _qOpt) {
          // Клиент может настраивать запрос с помощью must/should/mustNot.
          oe.must match {
            case None =>
              nq.should(_q)
              shouldClauses += 1
            case Some(true) =>
              nq.must(_q)
            case _ =>
              nq.mustNot(_q)
          }
        }
        if (_qOpt.isEmpty)
          LOGGER.warn("edge.NestedSearch: suppressed empty bool query for " + oe)
      }

      // Если should-clause'ы отсутствуют, то minimum should match 0. Иначе 1.
      nq.minimumNumberShouldMatch(
        Math.max(1, shouldClauses)
      )

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
