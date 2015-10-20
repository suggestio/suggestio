package io.suggest.model.n2.geo.search

import io.suggest.model.geo.GeoDistanceQuery
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
  def gsShapes: Seq[GeoDistanceQuery]


  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt

    val levels = gsLevels
    val shapes = gsShapes

    if (levels.isEmpty && shapes.isEmpty) {
      qbOpt0

    } else {

      // Есть какие-то критерии поиска. Сразу пытаемся искать по шейпам...
      val nq: QueryBuilder = if (shapes.nonEmpty) {
        val levels1 = if (levels.isEmpty) NodeGeoLevels.values else levels
        val queriesIter = for {
          shape   <- shapes.iterator
          glevel  <- levels1.iterator
        } yield {
          val shapeFn = MNodeFields.Geo.geoShapeFn( glevel )
          // Собраьт outer circle query
          val gq = QueryBuilders.geoShapeQuery(shapeFn, shape.outerCircle.toEsShapeBuilder)
          // Просверлить дырку, если требуется.
          shape.innerCircleOpt.fold[QueryBuilder](gq) { inCircle =>
            // TODO Этот фильтр скорее всего не пашет, т.к. ни разу не тестировался и уже пережил ДВА перепиливания подсистемы географии.
            val innerFilter = FilterBuilders.geoDistanceFilter( MNodeFields.Geo.POINT_FN )
              .point(inCircle.center.lat, inCircle.center.lon)
              .distance(inCircle.radius.distance, inCircle.radius.units)
            val notInner = FilterBuilders.notFilter(innerFilter)
            QueryBuilders.filteredQuery(gq, notInner)
          }
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

      } else {
        // Нет шейпов, это значит есть уровни.
        val fn = MNodeFields.Geo.SHAPE_GLEVEL_FN
        QueryBuilders.termsQuery(fn, levels.map(_.esfn): _*)
      }

      // Завернуть собранную инфу в nested-запрос и накатить на исходную query.
      qbOpt0 map { qb0 =>
        val gqNf = FilterBuilders.nestedFilter(MNodeFields.Geo.SHAPE_FN, nq)
        QueryBuilders.filteredQuery(qb0, gqNf)

      } orElse {
        val qb2 = QueryBuilders.nestedQuery(MNodeFields.Geo.SHAPE_FN, nq)
        Some(qb2)
      }
    }
  }

}


trait GeoShapeIntersectDflt extends GeoShapeIntersect {
  override def gsLevels: Seq[NodeGeoLevel]      = Nil
  override def gsShapes: Seq[GeoDistanceQuery]  = Nil
}


trait GeoShapeIntersectWrap extends GeoShapeIntersect with DynSearchArgsWrapper {
  override type WT <: GeoShapeIntersect
  override def gsLevels = _dsArgsUnderlying.gsLevels
  override def gsShapes = _dsArgsUnderlying.gsShapes
}
