package io.suggest.model.n2.geo.search

import io.suggest.model.geo.IToEsQueryFn
import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import io.suggest.ym.model.{NodeGeoLevels, NodeGeoLevel}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.10.15 16:58
 * Description: Поиск по пересечению с гео-фигурой.
 * Обычно используется для матчинга любой хранимой фигуры с кругом.
 */

trait GeoShapeIntersect extends DynSearchArgs {

  /** Есть указанный/любой шейп на указанных гео-уровнях. */
  def gsLevels: Seq[NodeGeoLevel]

  /** Геошейпы, по которым нужно матчить. */
  def gsShapes: Seq[IToEsQueryFn]

  /** Искать/фильтровать по флагу совместимости с GeoJSON. */
  def gsGeoJsonCompatible: Option[Boolean]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt

    val levels = gsLevels
    val shapes = gsShapes
    val gjsCompatOpt = gsGeoJsonCompatible

    if (levels.isEmpty && shapes.isEmpty && gjsCompatOpt.isEmpty) {
      qbOpt0

    } else {

      /** Добавить в query фильтр по флагу */
      def _withGjsCompatFilter(qb0: QueryBuilder): QueryBuilder = {
        gjsCompatOpt.fold(qb0) { gjsCompat =>
          val gjsFr = FilterBuilders.termFilter(MNodeFields.Geo.GEO_JSON_COMPATIBLE_FN, gjsCompat)
          QueryBuilders.filteredQuery(qb0, gjsFr)
        }
      }

      // Есть какие-то критерии поиска. Сразу пытаемся искать по шейпам...
      val nq: QueryBuilder = if (shapes.nonEmpty) {
        val levels1 = if (levels.isEmpty) NodeGeoLevels.values else levels
        val queriesIter = for {
          shape   <- shapes.iterator
          glevel  <- levels1.iterator
        } yield {
          val shapeFn = MNodeFields.Geo.geoShapeFn( glevel )
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

      } else if (levels.nonEmpty) {
        // Нет шейпов, это значит есть уровни.
        val fn = MNodeFields.Geo.SHAPE_GLEVEL_FN
        val qb0 = QueryBuilders.termsQuery(fn, levels.map(_.esfn): _*)
        _withGjsCompatFilter(qb0)

      } else {
        // Нужно искать по флагу совместимости с GeoJSON.
        val gjsCompat = gjsCompatOpt.get
        QueryBuilders.termQuery(MNodeFields.Geo.GEO_JSON_COMPATIBLE_FN, gjsCompat)
      }

      // Завернуть собранную инфу в nested-запрос и накатить на исходную query.
      val fn = MNodeFields.Geo.SHAPE_FN
      qbOpt0 map { qb0 =>
        val gqNf = FilterBuilders.nestedFilter(fn, nq)
        QueryBuilders.filteredQuery(qb0, gqNf)
      } orElse {
        val qb2 = QueryBuilders.nestedQuery(fn, nq)
        Some(qb2)
      }
    }
  }

}


trait GeoShapeIntersectDflt extends GeoShapeIntersect {
  override def gsLevels: Seq[NodeGeoLevel] = Nil
  override def gsShapes: Seq[IToEsQueryFn] = Nil
  override def gsGeoJsonCompatible: Option[Boolean] = None
}


trait GeoShapeIntersectWrap extends GeoShapeIntersect with DynSearchArgsWrapper {
  override type WT <: GeoShapeIntersect
  override def gsLevels = _dsArgsUnderlying.gsLevels
  override def gsShapes = _dsArgsUnderlying.gsShapes
  override def gsGeoJsonCompatible = _dsArgsUnderlying.gsGeoJsonCompatible
}
