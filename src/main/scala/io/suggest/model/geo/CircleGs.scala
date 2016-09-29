package io.suggest.model.geo

import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.json._
import play.api.libs.functional.syntax._

import GeoShape.COORDS_ESFN
import play.extras.geojson.{LatLng, Point}

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
    (__ \ COORDS_ESFN).format[GeoPoint] and
    (__ \ RADIUS_ESFN).format[Distance]
  )(apply, unlift(unapply))

}


import CircleGs._


case class CircleGs(center: GeoPoint, radius: Distance) extends GeoShapeQuerable {

  override def shapeType = GsTypes.circle

  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(
      COORDS_ESFN  -> center.toPlayGeoJson,
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
  override def toPlayGeoJsonGeom: Point[LatLng] = {
    PointGs(center).toPlayGeoJsonGeom
  }

}

