package io.suggest.geo

import io.suggest.geo.GeoShape.COORDS_ESFN
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, Point}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:29
 * Description: Круг в двумерном пространстве.
 */
object CircleGs extends GsStatic {

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

}


import io.suggest.geo.CircleGs._


case class CircleGs(center: MGeoPoint, radius: Distance) extends GeoShapeQuerable {

  override def shapeType = GsTypes.circle

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

  /** Circle представляется точкой, т.к. GeoJSON не поддерживает Circle. */
  override def toPlayGeoJsonGeom: Point[LngLat] = {
    PointGs(center).toPlayGeoJsonGeom
  }

}

