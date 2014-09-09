package io.suggest.model.geo

import io.suggest.ym.model.NodeGeoLevels.NodeGeoLevel
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.unit.DistanceUnit
import DistanceUnit.{Distance => EsDistance}
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilder, FilterBuilders, QueryBuilders}

object Distance {

  val parseDistance: PartialFunction[Any, Distance] = {
    case s: String =>
      val esDistance = EsDistance.parseDistance(s)
      Distance(esDistance)

    case esDistance: EsDistance =>
      Distance(esDistance)

    case dist: Distance =>
      dist
  }

  def apply(esDistance: EsDistance): Distance = {
    Distance(esDistance.value, esDistance.unit)
  }
}


/** Описание дистанции. */
case class Distance(distance: Double, units: DistanceUnit) {
  override def toString = units.toString(distance)

  def toEsDistance = new EsDistance(distance, units)
}


/** Описание того, как надо фильтровать по дистанции относительно какой-то точки на поверхности планеты. */
case class GeoDistanceQuery(center: GeoPoint, distanceMin: Option[Distance], distanceMax: Distance) {

  def outerCircle = CircleGs(center, distanceMax)

  def innerCircleOpt = distanceMin.map { CircleGs(center, _) }

}


case class GeoShapeQueryData(gdq: GeoDistanceQuery, glevel: NodeGeoLevel)


/** Для описания "координат" расположения индексированного геошейпа и поиска по нему, используем сие творение. */
trait GeoShapeIndexed {
  def _index: String
  def _type: String
  def _id: String
  def name: String
  def path: Option[String] = Some(name)

  def toGeoShapeQuery: QueryBuilder = {
    val qb = QueryBuilders.geoShapeQuery(name, _id, _type)
      .indexedShapeIndex(_index)
    if (path.isDefined)
      qb.indexedShapePath(path.get)
    qb
  }

  def toGeoShapeFilter: FilterBuilder = {
    val fb = FilterBuilders.geoShapeFilter(name, _id, _type, ShapeRelation.INTERSECTS)
      .indexedShapeIndex(_index)
    if (path.isDefined)
      fb.indexedShapePath(path.get)
    fb
  }
}
