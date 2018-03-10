package io.suggest.geo

import org.elasticsearch.common.geo.builders._
import play.api.libs.json._
import au.id.jazzy.play.geojson.{LngLat, Polygon}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 13:20
 * Description: Sio-класс для полигона.
 */

object PolygonGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = PolygonGs

  override def toPlayGeoJsonGeom(pgs: Shape_t): Polygon[LngLat] = {
    Polygon(
      coordinates = {
        pgs.outerWithHoles
          .iterator
          .map { lsgs =>
            LineStringGsJvm.toPlayGeoJsonGeom(lsgs).coordinates
          }
          .toStream
      }
    )
  }

  override def toEsShapeBuilder(gs: Shape_t): PolygonBuilder = {
    val inner = new CoordinatesBuilder()
      .coordinates( MultiPointGsJvm.geoPoints2esCoords(gs.outer.coords) )
      .close()

    gs.holes.foldLeft( ShapeBuilders.newPolygon( inner ) ) { (pb, lsGs) =>
      val lsb = LineStringGsJvm.toEsShapeBuilder( lsGs )
      pb.hole( lsb, true )
    }
  }

}
