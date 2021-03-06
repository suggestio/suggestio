package io.suggest.geo

import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.common.unit.DistanceUnit.{Distance => EsDistance}
import org.elasticsearch.geometry.Geometry
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
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

  def meters(distance: Double): Distance = {
    apply(distance, DistanceUnit.METERS)
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
    case _ =>
      JsError("expected.jsstring")
  }

  val WRITES = Writes[Distance] { dst =>
    JsString( dst.toString )
  }

  implicit val FORMAT = Format(READS, WRITES)

}


/** Описание дистанции. */
final case class Distance(distance: Double, units: DistanceUnit) {

  override def toString = units.toString(distance)

  def toEsDistance = new EsDistance(distance, units)

  /** Вернуть значение distance в указанных единицах измерения. */
  def distanceIn(units2: DistanceUnit) = DistanceUnit.convert(distance, units, units2)

  /** Вернуть значение distance в метрах. */
  def meters      = distanceIn( DistanceUnit.METERS )

  /** Вернуть значение distance в километрах. */
  def kiloMeters  = distanceIn( DistanceUnit.KILOMETERS )

}


trait IToEsQueryFn {
  def toEsQuery(fn: String): QueryBuilder
}

case class GeoShapeToEsQuery( gs: IGeoShapeQuerable ) extends IToEsQueryFn {

  def esShapeBuilder: Geometry = GeoShapeJvm.toEsShapeBuilder(gs)

  override def toEsQuery(fn: String): QueryBuilder =
    QueryBuilders.geoShapeQuery( fn, esShapeBuilder )

  override def toString = gs.toString

}
