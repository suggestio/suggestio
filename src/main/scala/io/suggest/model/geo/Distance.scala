package io.suggest.model.geo

import io.suggest.model.n2.node.MNodeFields
import io.suggest.ym.model.NodeGeoLevel
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.unit.DistanceUnit
import DistanceUnit.{Distance => EsDistance}
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilder, FilterBuilders, QueryBuilders}
import play.api.libs.json._

object Distance {

  val parseDistance: PartialFunction[Any, Distance] = {
    case s: String =>
      apply(s)
    case esDistance: EsDistance =>
      apply(esDistance)
    case dist: Distance =>
      dist
  }

  def apply(esDistance: EsDistance): Distance = {
    Distance(esDistance.value, esDistance.unit)
  }

  def apply(raw: String): Distance = {
    val esDistance = EsDistance.parseDistance(raw)
    Distance(esDistance)
  }

  val READS = Reads[Distance] {
    case JsString(raw) =>
      JsSuccess(apply(raw))
    case other =>
      JsError("expected.jsstring")
  }

  val WRITES = Writes[Distance] { dst =>
    JsString( dst.toString )
  }

  implicit val FORMAT = Format(READS, WRITES)

}


/** Описание дистанции. */
case class Distance(distance: Double, units: DistanceUnit) {
  override def toString = units.toString(distance)

  def toEsDistance = new EsDistance(distance, units)
}


trait IToEsQueryFn {
  def toEsQuery(fn: String): QueryBuilder
}


/** Описание того, как надо фильтровать по дистанции относительно какой-то точки на поверхности планеты. */
case class GeoDistanceQuery(
  center      : GeoPoint,
  distanceMin : Option[Distance],
  distanceMax : Distance
)
  extends IToEsQueryFn
{

  def outerCircle = CircleGs(center, distanceMax)

  def innerCircleOpt = distanceMin.map { CircleGs(center, _) }

  override def toEsQuery(fn: String): QueryBuilder = {
    val gq = QueryBuilders.geoShapeQuery(fn, outerCircle.toEsShapeBuilder)
    // Просверлить дырку, если требуется.
    innerCircleOpt.fold[QueryBuilder](gq) { inCircle =>
      // TODO Этот фильтр скорее всего не пашет, т.к. ни разу не тестировался и уже пережил ДВА перепиливания подсистемы географии.
      val innerFilter = FilterBuilders.geoDistanceFilter( MNodeFields.Geo.POINT_FN )
        .point(inCircle.center.lat, inCircle.center.lon)
        .distance(inCircle.radius.distance, inCircle.radius.units)
      val notInner = FilterBuilders.notFilter(innerFilter)
      QueryBuilders.filteredQuery(gq, notInner)
    }
  }

}


case class GeoShapeQueryData(gdq: GeoDistanceQuery, glevel: NodeGeoLevel)


/** Для описания "координат" расположения индексированного геошейпа и поиска по нему, используем сие творение. */
trait IGeoShapeIndexed {
  def _index: String
  def _type: String
  def _id: String
  def name: String
  def path: String

  def toGeoShapeQuery: QueryBuilder = {
    QueryBuilders.geoShapeQuery(name, _id, _type)
      .indexedShapeIndex(_index)
      .indexedShapePath(path)
  }

  def toGeoShapeFilter: FilterBuilder = {
    FilterBuilders.geoShapeFilter(name, _id, _type, ShapeRelation.INTERSECTS)
      .indexedShapeIndex(_index)
      .indexedShapePath(path)
  }

  override def toString: String = {
    classOf[IGeoShapeIndexed].getSimpleName + "(" + _index + "/" + _type + "/" + _id + "," + name + "," + path + ")"
  }
}
