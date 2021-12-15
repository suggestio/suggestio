package io.suggest.geo

import org.elasticsearch.geometry.{MultiPolygon => EsMultiPolygon}
import au.id.jazzy.play.geojson.{LngLat, MultiPolygon}
import org.locationtech.spatial4j.shape.Shape

import scala.jdk.CollectionConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:58
 * Description: Мультиполигон - это список полигонов.
 */
object MultiPolygonGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = MultiPolygonGs

  override def toPlayGeoJsonGeom(mpGs: Shape_t): MultiPolygon[LngLat] = {
    MultiPolygon(
      coordinates = mpGs.polygons
        .iterator
        .map { pgs =>
          PolygonGsJvm.toPlayGeoJsonGeom( pgs ).coordinates
        }
        .toSeq
    )
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder(gs: Shape_t): EsMultiPolygon = {
    new EsMultiPolygon(
      gs.polygons
        .iterator
        .map { PolygonGsJvm.toEsShapeBuilder }
        .toList
        .asJava
    )
  }

  /** Convert to spatial4j shape. */
  override def toSpatialShape(gs: MultiPolygonGs): Shape = {
    val builder = GeoShapeJvm.S4J_CONTEXT.getShapeFactory.multiPolygon()

    for (poly <- gs.polygons)
      builder.add( PolygonGsJvm.toSpatialShapeBuilder(poly) )

    builder.build()
  }

}
