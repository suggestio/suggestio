package io.suggest.geo

import org.elasticsearch.geometry.{Point => EsPoint}
import au.id.jazzy.play.geojson.{LngLat, Point}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:20
 * Description: Sio-представление точки в двумерном пространстве WGS84.
 */

object PointGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = PointGs

  def toPlayGeoJsonGeom(mgp: MGeoPoint): Point[LngLat] = {
    Point( GeoPoint.toLngLat( mgp ) )
  }
  override def toPlayGeoJsonGeom(gs: Shape_t): Point[LngLat] = {
    toPlayGeoJsonGeom( gs.coord )
  }

  override def toEsShapeBuilder(gs: Shape_t): EsPoint =
    GeoPoint.toEsPoint( gs.coord )


  /** Convert to spatial4j shape. */
  override def toSpatialShape(gs: PointGs) = {
    GeoShapeJvm
      .S4J_CONTEXT
      .getShapeFactory
      .pointXY( gs.coord.lon.doubleValue, gs.coord.lat.doubleValue )
  }

}



