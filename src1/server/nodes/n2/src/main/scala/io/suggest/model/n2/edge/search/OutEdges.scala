package io.suggest.model.n2.edge.search

import io.suggest.es.model.{MWrapClause, QueryUtil}
import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.n2.node.MNodeFields.Edges._
import io.suggest.util.MacroLogsI
import io.suggest.ym.model.{NodeGeoLevel, NodeGeoLevels}
import org.elasticsearch.index.query.{MatchQueryBuilder, QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:45
 * Description: Поиск по nested-документам в out-эджах.
 */
trait OutEdges extends DynSearchArgs with MacroLogsI {

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
          val qmType = if (tcr.isPrefix)
            MatchQueryBuilder.Type.PHRASE_PREFIX
          else
            MatchQueryBuilder.Type.PHRASE
          // Собрать match query
          val tq = QueryBuilders.matchQuery(fn, tcr.face)
            .`type`(qmType)
            .zeroTermsQuery( MatchQueryBuilder.ZeroTermsQuery.ALL )
          MWrapClause(tcr.must, tq)
        }

        val tq = QueryUtil.maybeWrapToBool(tQueries)
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
          val levels1: Iterable[NodeGeoLevel] = if (gsi.levels.isEmpty)
            NodeGeoLevels.valuesT
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
            bq.minimumNumberShouldMatch(1)
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
        val gqNf = QueryBuilders.nestedQuery(fn, nq)
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
        _qOpt = _qOpt.map { _q =>
          val nodeIdsFilter = QueryBuilders.termsQuery(fn, oe.nodeIds: _*)
          QueryBuilders.boolQuery()
            .must(_q)
            .filter(nodeIdsFilter)
        }.orElse {
          val __q = QueryBuilders.termsQuery(fn, oe.nodeIds: _*)
          Some(__q)
        }
      }

      // Предикаты рёбер добавить через фильтр либо query.
      if (oe.predicates.nonEmpty) {
        val fn = EDGE_OUT_PREDICATE_FULL_FN
        val predIds = oe.predicates.map(_.strId)
        _qOpt = _qOpt.map { _q =>
          val predf = QueryBuilders.termsQuery(fn, predIds: _*)
          QueryBuilders.boolQuery()
            .must(_q)
            .filter(predf)
        }.orElse {
          val _q = QueryBuilders.termsQuery(fn, predIds: _*)
          Some(_q)
        }
      }

      // ad search receivers: добавить show levels
      if (oe.anySl.nonEmpty) {
        // Требуется, чтобы был хотя бы один show level. Любой какой-нибудь, но обязательно был.
        if (_qOpt.nonEmpty && oe.sls.isEmpty) {
          // missing/existing filter можно навешивать только если уже есть тело nested query
          val fn = EDGE_OUT_INFO_SLS_FN
          val eq = QueryBuilders.existsQuery(fn)
          val _nq2 = {
            val qq = QueryBuilders.boolQuery()
              .must(_qOpt.get)
            if (oe.anySl.get) {
              qq.filter(eq)
            } else {
              qq.mustNot(eq)
            }
          }

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
          val slsf = QueryBuilders.termsQuery(slFn, slsStr : _*)
          QueryBuilders.boolQuery()
            .must(_q)
            .filter(slsf)
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
          val flagFl = QueryBuilders.termQuery(flagFn, flag)
          QueryBuilders.boolQuery
            .must(_q)
            .filter(flagFl)
        }.orElse {
          val _q = QueryBuilders.termQuery(flagFn, flag)
          Some(_q)
        }
      }

      if (_qOpt.isEmpty)
        LOGGER.warn("edge.NestedSearch: suppressed empty bool query for " + oe)

      for (_q <- _qOpt) yield {
        val _qn = QueryBuilders.nestedQuery(nestPath, _q)
        MWrapClause(oe.must, _qn)
      }
    }
      .toStream

    // Объеденить запросы в единый запрос.
    QueryUtil.maybeWrapToBool(clauses)
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
