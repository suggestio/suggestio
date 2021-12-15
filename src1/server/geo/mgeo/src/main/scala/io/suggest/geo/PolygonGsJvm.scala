package io.suggest.geo

import org.elasticsearch.geometry.{Polygon => EsPolygon}
import au.id.jazzy.play.geojson.{LngLat, Polygon}
import org.locationtech.spatial4j.shape.Shape

import scala.jdk.CollectionConverters._

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
          .toSeq
      }
    )
  }

  override def toEsShapeBuilder(gs: Shape_t): EsPolygon = {
    new EsPolygon(
      LineStringGsJvm.toEsLinearRing( gs.outer ),
      gs.holes
        .iterator
        .map( LineStringGsJvm.toEsLinearRing )
        .toList
        .asJava
    )
  }


  def toSpatialShapeBuilder(gs: PolygonGs) = {
    val builder = GeoShapeJvm.S4J_CONTEXT.getShapeFactory.polygon()
    for (pt <- gs.outer.coords)
      builder.pointXY( pt.lon.doubleValue, pt.lat.doubleValue )

    for (hole <- gs.holes) {
      val holeBuilder = builder.hole()

      for (holePt <- hole.coords)
        holeBuilder.pointXY( holePt.lon.doubleValue, holePt.lat.doubleValue )

      holeBuilder.endHole()
    }

    builder
  }
  override def toSpatialShape(gs: PolygonGs): Shape =
    toSpatialShapeBuilder( gs ).build()

}
