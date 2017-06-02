package io.suggest.geo

import io.suggest.geo.GeoShape._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, Point}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:20
 * Description: Sio-представление точки в двумерном пространстве WGS84.
 */

object PointGs extends GsStaticJvm {

  override type Shape_t = PointGs

  /** play-json deserializer. */
  override def DATA_FORMAT: Format[PointGs] = {
    (__ \ COORDS_ESFN)
      .format( GeoPoint.FORMAT_GEO_ARRAY )
      .inmap(apply, unlift(unapply))
  }

  def toPlayGeoJsonGeom(mgp: MGeoPoint): Point[LngLat] = {
    Point( GeoPoint.toLngLat( mgp ) )
  }
  override def toPlayGeoJsonGeom(gs: Shape_t): Point[LngLat] = {
    toPlayGeoJsonGeom( gs.coord )
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val mgpJson = GeoPoint.toPlayGeoJson(gs.coord)
    (COORDS_ESFN -> mgpJson) :: Nil
  }

}


case class PointGs(coord: MGeoPoint) extends GeoShapeQuerable {

  override def shapeType = GsTypes.Point

  override def toEsShapeBuilder = {
    ShapeBuilder.newPoint(coord.lon, coord.lat)
  }

  override def firstPoint = coord

  override def centerPoint = Some(coord)

}


