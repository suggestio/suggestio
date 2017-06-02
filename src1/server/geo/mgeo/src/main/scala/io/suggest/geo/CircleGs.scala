package io.suggest.geo

import io.suggest.geo.GeoShape.COORDS_ESFN
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{Geometry, LngLat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:29
 * Description: Круг в двумерном пространстве.
 */
object CircleGs extends GsStaticJvm {

  override type Shape_t = CircleGs

  val RADIUS_ESFN = "radius"

  override def DATA_FORMAT: OFormat[CircleGs] = (
    (__ \ COORDS_ESFN).format[MGeoPoint] and
    (__ \ RADIUS_ESFN).format[Distance]
  )(apply, unlift(unapply))

  def apply(geoCircle: MGeoCircle): CircleGs = {
    CircleGs(
      center = geoCircle.center,
      radius = Distance.meters( geoCircle.radiusM )
    )
  }

  /** Вернуть инстанс круга из инстанса гео-шейпа.
    *
    * @param gs Какой-то [[GeoShape]].
    * @return Опциональный [[CircleGs]].
    *         None, если gs -- это НЕ круг, а что-либо другое.
    */
  def maybeFromGs(gs: GeoShape): Option[CircleGs] = {
    gs match {
      case circle: CircleGs   => Some(circle)
      case _                  => None
    }
  }

  /** Circle представляется точкой, т.к. GeoJSON не поддерживает Circle. */
  override def toPlayGeoJsonGeom(circle: CircleGs): Geometry[LngLat] = {
    PointGs.toPlayGeoJsonGeom( circle.center )
  }

}


import io.suggest.geo.CircleGs._


case class CircleGs(center: MGeoPoint, radius: Distance) extends GeoShapeQuerable {

  override def shapeType = GsTypes.Circle

  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(
      COORDS_ESFN  -> GeoPoint.toPlayGeoJson(center),
      RADIUS_ESFN  -> JsString(radius.toString)
    )
  }

  override def toEsShapeBuilder = {
    ShapeBuilder.newCircleBuilder()
      .center(center.lon, center.lat)
      .radius(radius.distance, radius.units)
  }

  override def firstPoint = center

  override def centerPoint = Some(center)

}

