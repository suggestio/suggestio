package io.suggest.model.n2.geo.search

import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.search.{DynSearchArgs, DynSearchArgsWrapper}
import org.elasticsearch.index.query.{FilterBuilder, FilterBuilders, QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 21:59
  * Description: Фильтрация по наличию геоточки.
  */
trait HasGeoPoint extends DynSearchArgs {

  /** Опциональный флаг наличия/отсутствия геоточки у документа. */
  def hasGeoPoint: Option[Boolean]

  override def toEsQuery: QueryBuilder = {
    val q0 = super.toEsQuery
    hasGeoPoint.fold(q0) { has =>
      val fn = MNodeFields.Geo.POINT_FN
      val filter: FilterBuilder = if (has) {
        FilterBuilders.existsFilter(fn)
      } else {
        FilterBuilders.missingFilter(fn)
      }
      QueryBuilders.filteredQuery(q0, filter)
    }
  }


  override def sbInitSize: Int = {
    collStringSize(hasGeoPoint, super.sbInitSize)
  }
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("hasGeoPoint", hasGeoPoint, super.toStringBuilder)
  }

}


/** Дефолтовая реализация полей [[HasGeoPoint]]. */
trait HasGeoPointDflt extends HasGeoPoint {
  override def hasGeoPoint: Option[Boolean] = None
}


/** Wrapper-реализация полей [[HasGeoPoint]]. */
trait HasGeoPointWrap extends HasGeoPoint with DynSearchArgsWrapper {
  override type WT <: HasGeoPoint
  override def hasGeoPoint = _dsArgsUnderlying.hasGeoPoint
}
