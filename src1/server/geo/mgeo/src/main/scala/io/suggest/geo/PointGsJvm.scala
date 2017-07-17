package io.suggest.geo

import io.suggest.geo.GeoShapeJvm._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilders
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, Point}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:20
 * Description: Sio-представление точки в двумерном пространстве WGS84.
 */

object PointGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = PointGs

  /** play-json deserializer. */
  override def DATA_FORMAT: Format[PointGs] = {
    (__ \ COORDS_ESFN)
      .format( MGeoPoint.FORMAT_GEO_ARRAY )
      .inmap(PointGs.apply, unlift(PointGs.unapply))
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

  override def toEsShapeBuilder(gs: Shape_t) = {
    val jCoord = GeoPoint.toJtsCoordinate( gs.coord )
    ShapeBuilders.newPoint( jCoord )
  }

}



