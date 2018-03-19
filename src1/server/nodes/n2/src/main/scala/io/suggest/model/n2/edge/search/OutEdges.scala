package io.suggest.model.n2.edge.search

import io.suggest.es.model.{MWrapClause, IMust}
import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.geo.{MNodeGeoLevel, MNodeGeoLevels}
import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.n2.node.MNodeFields.Edges._
import io.suggest.util.logs.IMacroLogs
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:45
 * Description: Поиск по nested-документам в out-эджах.
 */
trait OutEdges extends DynSearchArgs with IMacroLogs {

  /** Поиск/фильтрация по out-эджам согласно описанным критериям. */
  def outEdges: Seq[ICriteria]

  private def _crs2query(crs: TraversableOnce[ICriteria]): QueryBuilder = {

    val nestPath = E_OUT_FN

    // Сборка nested queries.
    val clauses = crs.flatMap { oe =>

      // Сюда будет аккамулироваться финальный поисковый запрос:
      var _qOpt: Option[QueryBuilder] = None

      // В первую очередь ищем по fts в рамках текущего nested edge. А именно: по тегам с match-query наперевес. match filter вроде не особо работает.
      if (oe.tags.nonEmpty) {
        val tQueries = for (tcr <- oe.tags) yield {
          // Узнаём имя поля для матчинга тега.
          val fn = if (tcr.exact)
            E_OUT_INFO_TAGS_RAW_FN
          else
            E_OUT_INFO_TAGS_FN
          // Узнать желаемый тип запроса
          val tq = if (tcr.isPrefix)
            QueryBuilders.matchPhrasePrefixQuery(fn, tcr.face)
          else
            QueryBuilders.matchPhraseQuery(fn, tcr.face)
          // Собрать match query
          MWrapClause(tcr.must, tq)
        }

        val tq = IMust.maybeWrapToBool(tQueries)
        // Первый в списке if, поэтому сразу как Some().
        _qOpt = Some(tq)
      }


      // Отрабатываем гео-шейпы, там тоже очень желательны query вместо filter.
      // Явно работать с Option API, чтобы избежать скрытых логических ошибок при смене Option на Seq.
      if (oe.gsIntersect.isDefined) {
        val gsi = oe.gsIntersect.get

        /** Добавить в query фильтр по флагу */
        def _withGjsCompatFilter(qb0: QueryBuilder): QueryBuilder = {
          gsi.gjsonCompat.fold(qb0) { gjsCompat =>
            val gjsFr = QueryBuilders.termQuery(MNodeFields.Edges.E_OUT_INFO_GS_GJSON_COMPAT_FN, gjsCompat)
            QueryBuilders.boolQuery()
              .must(qb0)
              .filter(gjsFr)
          }
        }

        // Есть какие-то критерии поиска. Сразу пытаемся искать по шейпам...
        val nq: QueryBuilder = if (gsi.shapes.nonEmpty) {
          val levels1: Iterable[MNodeGeoLevel] = if (gsi.levels.isEmpty)
            MNodeGeoLevels.values
          else
            gsi.levels
          val queriesIter = for {
            shape   <- gsi.shapes.iterator
            glevel  <- levels1.iterator
          } yield {
            val shapeFn = MNodeFields.Edges.E_OUT_INFO_GS_SHAPE_FN( glevel )
            val qb0 = shape.toEsQuery(shapeFn)
            _withGjsCompatFilter(qb0)
          }
          // Объединяем сгенеренные queries в одну.
          val queries = queriesIter.toStream
          if (queries.tail.isEmpty) {
            queries.head
          } else {
            val bq = QueryBuilders.boolQuery()
            for (q <- queries) {
              bq.should(q)
            }
            bq.minimumShouldMatch( 1 )
            bq
          }

        } else if (gsi.levels.nonEmpty) {
          // Нет шейпов, это значит есть уровни.
          val fn = MNodeFields.Edges.E_OUT_INFO_GS_GLEVEL_FN
          val qb0 = QueryBuilders.termsQuery(fn, gsi.levels.map(_.esfn): _*)
          _withGjsCompatFilter(qb0)

        } else {
          // Нужно искать по флагу совместимости с GeoJSON.
          val gjsCompat = gsi.gjsonCompat.get
          QueryBuilders.termQuery(MNodeFields.Edges.E_OUT_INFO_GS_GJSON_COMPAT_FN, gjsCompat)
        }

        // Завернуть собранную инфу в nested-запрос и накатить на исходную query.
        val fn = MNodeFields.Edges.E_OUT_INFO_GS_FN
        val gqNf = QueryBuilders.nestedQuery(fn, nq, ScoreMode.Max)
        _qOpt = _qOpt.map { qb0 =>
          QueryBuilders.boolQuery()
            .must(qb0)
            .filter(gqNf)
        }.orElse {
          Some(gqNf)
        }
      }


      // Поиск по id узлов, на которые указывают эджи.
      if (oe.nodeIds.nonEmpty) {
        val fn = EDGE_OUT_NODE_ID_FULL_FN

        val nodeIdsQb: QueryBuilder = if (oe.nodeIdsMatchAll) {
          // AND-матчинг всех пересленных nodeIds одновременно
          val nodeIdsBq = QueryBuilders.boolQuery()
          for (nodeId <- oe.nodeIds)
            nodeIdsBq.must( QueryBuilders.termQuery(fn, nodeId) )
          nodeIdsBq
        } else  {
          // OR-матчинг всех nodeIds
          QueryBuilders.termsQuery(fn, oe.nodeIds: _*)
        }

        _qOpt = _qOpt.map { _q =>
          QueryBuilders.boolQuery()
            .must(_q)
            .must(nodeIdsQb)
        }.orElse {
          Some( nodeIdsQb )
        }
      }

      // Предикаты рёбер добавить через фильтр либо query.
      if (oe.predicates.nonEmpty) {
        val fn = EDGE_OUT_PREDICATE_FULL_FN
        val predIds = oe.predicates.map(_.strId)
        val predf = QueryBuilders.termsQuery(fn, predIds: _*)
        _qOpt = _qOpt.map { _q =>
          QueryBuilders.boolQuery()
            .must(_q)
            .filter(predf)
        }.orElse {
          Some(predf)
        }
      }

      // Ищем/фильтруем по info.flag
      if (oe.flag.nonEmpty) {
        val flag = oe.flag.get
        val flagFn = EDGE_OUT_INFO_FLAG_FN
        val flagFl = QueryBuilders.termQuery(flagFn, flag)
        _qOpt = _qOpt.map { _q =>
          QueryBuilders.boolQuery
            .must(_q)
            .filter(flagFl)
        }.orElse {
          Some( flagFl )
        }
      }

      if (_qOpt.isEmpty)
        LOGGER.warn("edge.NestedSearch: suppressed empty bool query for " + oe)

      for (_q <- _qOpt) yield {
        // TODO ScoreMode.Avg -- с потолка взято, надо разобраться на тему оптимального варианта.
        val _qn = QueryBuilders.nestedQuery(nestPath, _q, ScoreMode.Avg)
        MWrapClause(oe.must, _qn)
      }
    }
      .toStream

    // Объеденить запросы в единый запрос.
    IMust.maybeWrapToBool(clauses)
  }


  /** Накатить эти сложные критерии на query. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val _outEdgesIter = outEdges
      .iterator
      .filter { _.nonEmpty }

    val qbOpt0 = super.toEsQueryOpt

    if (_outEdgesIter.isEmpty) {
      qbOpt0

    } else {
      val qb2 = _crs2query(_outEdgesIter)
      // Сборка основной query
      qbOpt0.map { qb0 =>
        QueryBuilders.boolQuery()
          .must(qb0)
          .filter(qb2)
      }.orElse {
        Some(qb2)
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

