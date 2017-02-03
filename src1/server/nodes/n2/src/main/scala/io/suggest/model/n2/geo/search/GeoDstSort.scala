package io.suggest.model.n2.geo.search

import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.geo.{GeoPoint, MGeoPoint}
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 18:16
 * Description: Поисковый аддон для сортировки результатов по дистанции
 * до указанной точки вместо алфавита.
 */
trait GeoDstSort extends DynSearchArgs {

  /** + сортировка результатов по расстоянию до указанной точки. */
  def withGeoDistanceSort: Option[MGeoPoint]

  override def toEsQuery: QueryBuilder = {
    val qb0 = super.toEsQuery
    withGeoDistanceSort.fold(qb0) { geoPoint =>
      val fn = MNodeFields.Geo.POINT_FN
      val func = ScoreFunctionBuilders
        .gaussDecayFunction(fn, GeoPoint.toEsStr(geoPoint), "1km")
        .setOffset("0km")
      QueryBuilders.functionScoreQuery(qb0, func)
        .boostMode(CombineFunction.REPLACE)
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withGeoDistanceSort, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("geoDstSort", withGeoDistanceSort, super.toStringBuilder)
  }
}


/** Дефолтовая реализация аддона [[GeoDstSort]]. */
trait GeoDstSortDflt extends GeoDstSort {
  override def withGeoDistanceSort: Option[MGeoPoint] = None
}


/** Дефолтовая реализация аддона [[GeoDstSort]]. */
trait GeoDstSortWrap extends GeoDstSort with DynSearchArgsWrapper {
  override type WT <: GeoDstSort
  override def withGeoDistanceSort = _dsArgsUnderlying.withGeoDistanceSort
}
