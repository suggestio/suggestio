package io.suggest.n2.edge.search

import io.suggest.es.model.{IMust, MEsNestedSearch, MWrapClause}
import io.suggest.es.search.DynSearchArgs
import io.suggest.geo.{MGeoPoint, MNodeGeoLevel, MNodeGeoLevels}
import io.suggest.n2.node.MNodeFields
import io.suggest.util.logs.MacroLogsImpl
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.lucene.search.function.{CombineFunction, FiltersFunctionScoreQuery}
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders, RangeQueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:45
 * Description: Поиск по nested-документам в out-эджах.
 */
object OutEdges extends MacroLogsImpl {

  private def _isToAssignQueryName = LOGGER.underlying.isTraceEnabled

  /** Сборка edge-критериев в nested query. */
  private def _crs2query(crs: IterableOnce[Criteria], outEdges: MEsNestedSearch[Criteria]): QueryBuilder = {
    val EF = MNodeFields.Edges
    val withQname = _isToAssignQueryName

    val clauses = (for {
      oe <- crs.iterator
      // Сборка nested queries.
      q <- {
        // TODO Opt Тут куча вложенных bool-query, а можно сделать одну bool-query. Это это будет проще и красивее.
        // Сюда будет аккамулироваться финальный поисковый запрос:
        var _qOpt: Option[QueryBuilder] = None

        // В первую очередь ищем по fts в рамках текущего nested edge. А именно: по тегам с match-query наперевес. match filter вроде не особо работает.
        if (oe.tags.nonEmpty) {
          val tQueries = for (tcr <- oe.tags) yield {
            // Узнаём имя поля для матчинга тега.
            val fn = if (tcr.exact)
              EF.EO_INFO_TAGS_RAW_FN
            else
              EF.EO_INFO_TAGS_FN

            // Узнать желаемый тип запроса
            val tq = if (tcr.isPrefix)
              QueryBuilders.matchPhrasePrefixQuery(fn, tcr.face)
            else
              QueryBuilders.matchPhraseQuery(fn, tcr.face)

            if (withQname)
              tq.queryName(s"oe.tag:$fn^=${tcr.face}")

            // Собрать match query
            MWrapClause(tcr.must, tq)
          }

          val tq = tQueries.toBoolQuery
          if (withQname && tq.isInstanceOf[BoolQueryBuilder])
            tq.queryName(s"oe.tags[${oe.tags.length}]")

          // Первый в списке if, поэтому сразу как Some().
          _qOpt = Some(tq)
        }


        // Отрабатываем гео-шейпы, там тоже очень желательны query вместо filter.
        // Явно работать с Option API, чтобы избежать скрытых логических ошибок при смене Option на Seq.
        if (oe.gsIntersect.isDefined) {
          // Завернуть собранную инфу в nested-запрос и накатить на исходную query.
          val fn = EF.EO_INFO_GS_FN
          val gqNf = {
            val gsi = oe.gsIntersect.get

            /** Добавить в query фильтр по флагу */
            def _withGjsCompatFilter(qb0: QueryBuilder): QueryBuilder = {
              gsi.gjsonCompat.fold(qb0) { gjsCompat =>
                val fn = EF.EO_INFO_GS_GJSON_COMPAT_FN
                val gjsFr = QueryBuilders.termQuery(fn, gjsCompat)
                if (withQname)
                  gjsFr.queryName(s"GeoJSON compat q: $fn=$gjsCompat")
                val q = QueryBuilders.boolQuery()
                  .must(qb0)
                  .filter(gjsFr)
                if (withQname)
                  q.queryName("bool with GeoJSON compat filter")
                q
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
                val shapeFn = EF.EO_INFO_GS_SHAPE_FN( glevel )
                val qb0 = shape.toEsQuery(shapeFn)
                if (withQname)
                  qb0.queryName(s"shape q: gnl=$glevel fn=$shapeFn shape=${shape.getClass.getSimpleName}")
                _withGjsCompatFilter(qb0)
              }
              // Объединяем сгенеренные queries в одну.
              val queries = queriesIter.toList
              if (queries.tail.isEmpty) {
                queries.head
              } else {
                val bq = QueryBuilders.boolQuery()
                for (q <- queries) {
                  bq.should(q)
                }
                bq.minimumShouldMatch( 1 )
                if (withQname)
                  bq.queryName(s"bool: ${queries.length} shapes")
                bq
              }

            } else if (gsi.levels.nonEmpty) {
              // Нет шейпов, это значит есть уровни.
              val fn = EF.EO_INFO_GS_GLEVEL_FN
              val levelNames = gsi.levels.map(_.esfn)
              val qb0 = QueryBuilders.termsQuery(fn, levelNames: _*)
              if (withQname)
                qb0.queryName(s"terms: ${fn} = ${levelNames.length}lvls: [${levelNames.mkString(", ")}]")
              _withGjsCompatFilter(qb0)

            } else {
              // Нужно искать по флагу совместимости с GeoJSON.
              val gjsCompat = gsi.gjsonCompat.get
              val q = QueryBuilders.termQuery(EF.EO_INFO_GS_GJSON_COMPAT_FN, gjsCompat)
              if (withQname)
                q.queryName(s"gjs compat: $gjsCompat")
              q
            }

            QueryBuilders.nestedQuery( fn, nq, ScoreMode.Max )
          }

          if (withQname)
            gqNf.queryName(s"nested: e.info.geoShape fn=${fn}")

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
          val fn = EF.EO_NODE_IDS_FN

          val nodeIdsQb: QueryBuilder = if (oe.nodeIdsMatchAll) {
            // AND-матчинг всех пересленных nodeIds одновременно
            val nodeIdsBq = QueryBuilders.boolQuery()
            for (nodeId <- oe.nodeIds) {
              val nq = QueryBuilders.termQuery(fn, nodeId)
              if (withQname)
                nq.queryName(s"e.nodeId: $fn=${nodeId}")
              nodeIdsBq.filter( nq )
            }
            if (withQname)
              nodeIdsBq.queryName(s"e.nodeIds[${oe.nodeIds.length}] filter")
            nodeIdsBq
          } else {
            // OR-матчинг всех nodeIds
            val orNodesQb = QueryBuilders.termsQuery(fn, oe.nodeIds: _*)
            if (withQname)
              orNodesQb.queryName(s"OR nodeIds[${oe.nodeIds.length}]: $fn=[${oe.nodeIds.mkString(", ")}]")
            orNodesQb
          }

          _qOpt = _qOpt.map { _q =>
            val qbb = QueryBuilders.boolQuery()
              .must(_q)
              .filter(nodeIdsQb)
            if (withQname)
              qbb.queryName(s"bool: nodeIds filter")
            qbb
          }.orElse {
            Some( nodeIdsQb )
          }
        }


        // Фильтрация по интервалу даты-времени.
        if (oe.date.nonEmpty) {
          val fn = MNodeFields.Edges.EO_INFO_DATE_FN

          val dateClauses = for {
            esRange <- oe.date
            if esRange.rangeClauses.nonEmpty
          } yield {
            val rangeQuery = esRange.rangeClauses
              .foldLeft( QueryBuilders.rangeQuery(fn) ) {
                (rq, rClause) =>
                  val f: Any => RangeQueryBuilder = rClause.op match {
                    case EsRangeOps.`<=` => rq.lte
                    case EsRangeOps.`<`  => rq.lt
                    case EsRangeOps.`>=` => rq.gte
                    case EsRangeOps.`>`  => rq.gt
                  }
                  f(rClause.value)
              }

            for (b <- esRange.boost)
              rangeQuery.boost( b )
            for (dtFmt <- esRange.dateFormat)
              rangeQuery.format(dtFmt)
            for (tz <- esRange.timeZone)
              rangeQuery.timeZone( tz.toString )

            MWrapClause(
              must          = esRange.must,
              queryBuilder  = rangeQuery,
            )
          }

          if (dateClauses.nonEmpty) {
            val qb = dateClauses.toBoolQuery
            _qOpt = _qOpt.map { qb0 =>
              QueryBuilders.boolQuery()
                .must( qb0 )
                .must( qb )
            }.orElse {
              Some(qb)
            }
          }
        }


        // Предикаты рёбер добавить через фильтр либо query.
        if (oe.predicates.nonEmpty) {
          val fn = EF.EO_PREDICATE_FN
          val predIds = oe.predicates
            .map(_.value)
          val predf = QueryBuilders.termsQuery(fn, predIds: _*)
          if (withQname)
            predf.queryName(s"predicate: $fn=${oe.predicates.length}[${oe.predicates.mkString(", ")}]")
          _qOpt = _qOpt.map { _q =>
            val qbp = QueryBuilders.boolQuery()
              .must(_q)
              .filter(predf)
            if (withQname)
              qbp.queryName("bool: predicates filter")
            qbp
          }.orElse {
            Some(predf)
          }
        }


        // Ищем/фильтруем по info.flag
        for (flag <- oe.flag) {
          val flagFn = EF.EO_INFO_FLAG_FN
          val flagFl = QueryBuilders.termQuery(flagFn, flag)
          if (withQname)
            flagFl.queryName(s"flag: $flagFn=$flag")
          _qOpt = _qOpt.map { _q =>
            QueryBuilders.boolQuery
              .must(_q)
              .filter(flagFl)
          }.orElse {
            Some( flagFl )
          }
        }


        // Сортировка по удалённости путём выставления score.
        for (mgp <- oe.geoDistanceSort) {
          val fn = EF.EO_INFO_GEO_POINTS_FN
          val geoPointStr = MGeoPoint.toEsStr(mgp)
          val scale = "100km"

          val func = ScoreFunctionBuilders.gaussDecayFunction(fn, geoPointStr, scale, 0, 0.5)
            .setWeight(10f)
          //.setOffset("0km") // TODO es-5.x А что надо тут выставить?
          val fq = _qOpt
            .fold( QueryBuilders.functionScoreQuery(func) ) { qb0 =>
              QueryBuilders.functionScoreQuery(qb0, func)
            }
            .boostMode( CombineFunction.REPLACE )
            .scoreMode( FiltersFunctionScoreQuery.ScoreMode.MAX )
            .boost(5f)
          if (withQname)
            fq.queryName(s"f-score-q: $fn $geoPointStr scale=$scale overInner?${_qOpt.nonEmpty}")
          _qOpt = Some(fq)
        }


        // Отработать фильтрацию по внешним сервисам:
        for ( extServices <- oe.extService ) {
          val fn = EF.EO_INFO_EXT_SERVICE_FN
          val qb9 = if (extServices.nonEmpty) {
            val extServicesIds = extServices.iterator.map(_.value).toSeq
            QueryBuilders.termsQuery(fn, extServicesIds: _*)
          } else {
            QueryBuilders.existsQuery(fn)
          }
          if (withQname)
            qb9.queryName( s"f-ext-services[${extServices.length}]" )
          val fq = _qOpt.fold [QueryBuilder](qb9) { qOpt0 =>
            QueryBuilders.boolQuery()
              .must( qOpt0 )
              .filter( qb9 )
          }
          _qOpt = Some(fq)
        }

        // Фильтрация по операционным системам.
        for (osFamilies <- oe.osFamilies) {
          val fn = EF.EO_INFO_OS_FAMILY_FN
          val qb9 = if (osFamilies.nonEmpty) {
            val osFamiliesIds = osFamilies.iterator.map(_.value).toSeq
            QueryBuilders.termsQuery(fn, osFamiliesIds: _*)
          } else {
            QueryBuilders.existsQuery(fn)
          }
          if (withQname)
            qb9.queryName( s"f-os-family[${osFamilies.length}]" )
          val fq = _qOpt.fold [QueryBuilder](qb9) { qOpt0 =>
            QueryBuilders.boolQuery()
              .must( qOpt0 )
              .filter( qb9 )
          }
          _qOpt = Some(fq)
        }

        // Отработать хэши при поиске файлов.
        if (oe.fileHashesHex.nonEmpty) {
          // Заданы хэш-суммы искомого файла. TODO Подготовить матчинг. Тут у нас nested search требуется..
          lazy val hashesTypeFn = EF.EO_MEDIA_FM_HASHES_TYPE_FN
          lazy val hashesValueFn = EF.EO_MEDIA_FM_HASHES_VALUE_FN
          lazy val nestedPath = EF.EO_MEDIA_FM_HASHES_FN

          val crQbs = (for {
            cr <- oe.fileHashesHex
            if cr.nonEmpty
          } yield {
            // Сборка одной query по одному критерию (внутри nested).
            val qb = QueryBuilders.boolQuery()

            // TODO Возможно, тут ошибка: все одновременно хэши быть не могут ведь? Надо сделать по аналогии с NodeIdSearch, где через пачку SHOULD сделано.
            if (cr.hTypes.nonEmpty) {
              val hTypesQb = QueryBuilders.termsQuery( hashesTypeFn, cr.hTypes.map(_.value): _* )
              qb.filter( hTypesQb )
            }

            if (cr.hexValues.nonEmpty) {
              val hValuesQb = QueryBuilders.termsQuery( hashesValueFn, cr.hexValues: _* )
              qb.filter( hValuesQb )
            }

            val qbNest = QueryBuilders.nestedQuery( nestedPath, qb, ScoreMode.None )

            MWrapClause(
              must          = cr.must,
              queryBuilder  = qbNest
            )
          })
            .to( LazyList )

          if (crQbs.nonEmpty) {
            // Объеденить все qb как must.
            val allCrsQb = crQbs.toBoolQuery

            if (withQname)
              allCrsQb.queryName( s"fileHashes[${oe.fileHashesHex.size}]" )

            // Сборка итоговой query
            _qOpt = _qOpt.map { qb0 =>
              QueryBuilders.boolQuery()
                .must( qb0 )
                .filter( allCrsQb )
            }.orElse {
              Some( allCrsQb )
            }
          }
        }


        // Поиск по MIME-типам.
        if (oe.fileMimes.nonEmpty) {
          // Указаны допустимые mime-типы, значит будем фильтровать:
          val fn = EF.EO_MEDIA_FM_MIME_FN

          // Т.к. нам нужен любой из списка допустимых mime-типов, надо делать пачку SHOULD clause:
          val nodeIdsWraps = for (mime <- oe.fileMimes) yield {
            MWrapClause(
              must = IMust.SHOULD,
              queryBuilder = QueryBuilders.termQuery( fn, mime )
            )
          }

          val mimesQb = nodeIdsWraps.toBoolQuery

          _qOpt = _qOpt.map { qb0 =>
            QueryBuilders.boolQuery()
              .must( qb0 )
              .filter( mimesQb )
          }.orElse {
            Some( mimesQb )
          }
        }


        // Поиск/фильтрация по имени файла.
        if (oe.fileSizeB.nonEmpty) {
          val fn = EF.EO_MEDIA_FM_SIZE_B_FN
          // Задан размер файла для поиска.
          val bsClauses = for (byteSize <- oe.fileSizeB) yield {
            MWrapClause(
              must          = IMust.SHOULD,
              queryBuilder  = QueryBuilders.termQuery( fn, byteSize )
            )
          }
          val qb2 = bsClauses.toBoolQuery

          _qOpt = _qOpt
            .map { qb0 =>
              QueryBuilders.boolQuery()
                .must(qb0)
                .filter(qb2)
            }
            .orElse {
              Some( qb2 )
            }
        }


        // Поиск/фильтрация по флагу fileMeta.isOriginal.
        for (fileIsOriginal <- oe.fileIsOriginal) {
          // Есть искомое значение флага. Собираем фильтр:
          val isOrigQb = QueryBuilders.termQuery( EF.EO_MEDIA_FM_IS_ORIGINAL_FN, fileIsOriginal )
          _qOpt = _qOpt
            .map { qb0 =>
              QueryBuilders.boolQuery()
                .must( qb0 )
                .filter( isOrigQb )
            }
            .orElse {
              Some(isOrigQb)
            }
        }


        // Поиск по критериям file-storage.
        if (oe.fileStorType.nonEmpty) {
          val qbStorType = QueryBuilders.termsQuery( EF.EO_MEDIA_STORAGE_TYPE_FN, oe.fileStorType.iterator.map(_.value).toSeq: _* )
          _qOpt = _qOpt
            .map { qb0 =>
              QueryBuilders.boolQuery()
                .must( qb0 )
                .must( qbStorType )
            }
            .orElse {
              Some( qbStorType )
            }
        }


        // Поиск по метаданным файлового хранилища.
        if (oe.fileStorMetaData.nonEmpty) {
          val qbStorMeta = QueryBuilders.termsQuery( EF.EO_MEDIA_STORAGE_DATA_META_FN, oe.fileStorMetaData.toSeq: _* )
          _qOpt = _qOpt
            .map { qb0 =>
              QueryBuilders.boolQuery()
                .must( qb0 )
                .must( qbStorMeta )
            }
            .orElse {
              Some( qbStorMeta )
            }
        }


        // Отработка фильтрации по шардам файлового хранилища.
        for (shards <- oe.fileStorShards) {
          val qb = QueryBuilders.boolQuery()
          _qOpt foreach qb.must

          val fn = EF.EO_MEDIA_STORAGE_DATA_SHARDS_FN
          if (shards.isEmpty)
            qb mustNot QueryBuilders.existsQuery( fn )
          else
            qb filter QueryBuilders.termsQuery( fn, shards.toSeq: _* )

          _qOpt = Some( qb )
        }


        // Поиск/фильтрация по флагам.
        if (oe.flags.nonEmpty) {
          val flagsQbs = (for {
            flagCr <- oe.flags.iterator
            if flagCr.flag.nonEmpty
          } yield {
            val flagCrQb = QueryBuilders.termsQuery(
              EF.EO_INFO_FLAGS_FLAG_FN,
              flagCr.flag
                .iterator
                .map(_.value)
                .toSeq: _*,
            )

            MWrapClause(
              must = IMust.SHOULD,
              queryBuilder = flagCrQb,
            )
          })
            .toList

          if (flagsQbs.nonEmpty) {
            _qOpt = _qOpt.map { qb0 =>
              (MWrapClause(IMust.MUST, qb0) :: flagsQbs)
                .toBoolQuery
            }.orElse {
              Some( flagsQbs.toBoolQuery )
            }
          }
        }

        // Сборка критерия для высоты или ширины.
        def __pictureWhCr(fn: => String, values: List[Int]): Unit = {
          if (values.nonEmpty) {
            val qb: QueryBuilder = values match {
              case exact :: Nil =>
                QueryBuilders.termQuery( fn, exact )
              // Кажется, что range не работает или работает не правильно:
              case from :: to :: Nil =>
                QueryBuilders.rangeQuery( fn )
                  .gte( from )
                  .lt( to )
              case terms =>
                QueryBuilders.termsQuery( fn, terms: _* )
            }
            _qOpt = _qOpt.map[QueryBuilder] { qb0 =>
              QueryBuilders.boolQuery()
                .must( qb0 )
                .must( qb )
            }.orElse {
              Some( qb )
            }
          }
        }
        __pictureWhCr( MNodeFields.Edges.EO_MEDIA_PICTURE_WH_WIDTH_FN, oe.pictureWidthPx )
        __pictureWhCr( MNodeFields.Edges.EO_MEDIA_PICTURE_WH_HEIGHT_FN, oe.pictureHeightPx )

        if (_qOpt.isEmpty)
          LOGGER.warn(s"empty bool query for $oe")

        _qOpt.iterator
      }
    } yield {
      val nestPath = EF.E_OUT_FN
      // TODO ScoreMode.Avg -- с потолка взято, надо разобраться на тему оптимального варианта.
      var _qn = QueryBuilders.nestedQuery(nestPath, q, ScoreMode.Max)

      for (esInnerHit <- outEdges.innerHits)
        _qn = _qn.innerHit( esInnerHit )

      // TODO Организовать сборку .innerHits().
      if (withQname)
        _qn.queryName(s"nested: $nestPath must?${oe.must} cr=$oe")

      MWrapClause(oe.must, _qn)
    })
      .toSeq

    // Объеденить запросы в единый запрос.
    clauses.toBoolQuery
  }

}


trait OutEdges extends DynSearchArgs {

  /** Поиск/фильтрация по out-эджам согласно описанным критериям. */
  def outEdges: MEsNestedSearch[Criteria] = MEsNestedSearch.empty


  /** Накатить эти сложные критерии на query. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val oes = outEdges
    val _outEdgesIter = oes
      .clauses
      .iterator
      .filter { _.nonEmpty }

    val qbOpt0 = super.toEsQueryOpt

    if (_outEdgesIter.isEmpty) {
      qbOpt0

    } else {
      val qb2 = OutEdges._crs2query(_outEdgesIter, oes)
      // Сборка основной query
      qbOpt0.map { qb0 =>
        val q = QueryBuilders.boolQuery()
          .must(qb0)
        if (oes.clauses.exists(_.isContainsSort)) {
          // не-filter, когда внутри _score
          q.must(qb2)
        } else {
          q.filter(qb2)
        }
      }.orElse {
        Some(qb2)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val s0 = super.sbInitSize
    s0 + outEdges.clauses.length * 80
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("outEdges", outEdges.clauses, super.toStringBuilder)
  }

}
