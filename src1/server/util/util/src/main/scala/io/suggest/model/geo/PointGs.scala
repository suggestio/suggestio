package io.suggest.model.geo

import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import GeoShape._
import io.suggest.geo.MGeoPoint
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{LatLng, Point}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:20
 * Description: Sio-представление точки в двумерном пространстве WGS84.
 */

object PointGs extends GsStatic {

  override type Shape_t = PointGs

  /** play-json deserializer. */
  override def DATA_FORMAT: Format[PointGs] = {
    (__ \ COORDS_ESFN)
      .format( GeoPoint.FORMAT_GEO_ARRAY )
      .inmap(apply, unlift(unapply))
  }

}


case class PointGs(coord: MGeoPoint) extends GeoShapeQuerable {

  override def shapeType = GsTypes.point

  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    (COORDS_ESFN -> GeoPoint.toPlayGeoJson(coord)) :: Nil
  }

  override def toEsShapeBuilder = {
    ShapeBuilder.newPoint(coord.lon, coord.lat)
  }

  override def firstPoint = coord

  override def toPlayGeoJsonGeom: Point[LatLng] = {
    Point( GeoPoint.toLatLng(coord) )
  }

  override def centerPoint = Some(coord)

}


