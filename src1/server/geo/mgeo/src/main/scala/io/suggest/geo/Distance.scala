package io.suggest.geo

import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.common.unit.DistanceUnit.{Distance => EsDistance}
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


/** Описание того, как надо фильтровать по дистанции относительно какой-то точки на поверхности планеты. */
final case class GeoDistanceQuery(
  center      : MGeoPoint,
  distanceMax : Distance
)
  extends IToEsQueryFn
{

  def outerCircle = CircleGs(center, radiusM = distanceMax.meters)

  override def toEsQuery(fn: String): QueryBuilder = {
    val geom = GeoShapeJvm
      .toEsShapeBuilder( outerCircle )
      .buildGeometry()

    QueryBuilders.geoShapeQuery( fn, geom )
    // util:2a9e4a872bff До 2016.jan.14 здесь жил ни разу не тестированный код вырезания inner-круга, т.е. distanceMin.
    // Он был удалён, т.к. был завязан на N2-утиль, которая должна жить в отдельном проекте. Да и он просто мертвый был.
  }

}
