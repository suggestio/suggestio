package io.suggest.model.n2.geo.search

import io.suggest.es.search.DynSearchArgs
import io.suggest.geo.{GeoPoint, MGeoPoint}
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
// TODO Эвакуировать код в OutEdges и удалить окончательно. Нигде не используется.
trait GeoDstSort extends DynSearchArgs {

  /** + сортировка результатов по расстоянию до указанной точки. */
  def withGeoDistanceSort: Option[MGeoPoint]

  override def toEsQuery: QueryBuilder = {
    val qb0 = super.toEsQuery
    withGeoDistanceSort.fold(qb0) { geoPoint =>
      val fn = "ttttt" // MNodeFields.Geo.POINT_FN
      val func = ScoreFunctionBuilders
        .gaussDecayFunction(fn, GeoPoint.toEsStr(geoPoint), "1km")
        //.setOffset("0km") // TODO es-5.x А что надо тут выставить?
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

