package io.suggest.geo

import org.elasticsearch.geometry.{MultiLine => EsMultiLine}
import au.id.jazzy.play.geojson.{LngLat, MultiLineString}
import org.locationtech.spatial4j.shape.Shape

import scala.jdk.CollectionConverters._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:32
 * Description: Представление multiline-объектов в рамках s.io.
 */

object MultiLineStringGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = MultiLineStringGs

  override def toPlayGeoJsonGeom(mlsGs: Shape_t): MultiLineString[LngLat] = {
    MultiLineString(
      coordinates = mlsGs.lines
        .iterator
        .map { lsGs =>
          LineStringGsJvm.toPlayGeoJsonGeom( lsGs ).coordinates
        }
        .toSeq
    )
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder(gs: Shape_t): EsMultiLine = {
    new EsMultiLine(
      gs.lines
        .iterator
        .map( LineStringGsJvm.toEsShapeBuilder )
        .toList
        .asJava
    )
  }

  override def toSpatialShape(gs: MultiLineStringGs): Shape = {
    val builder = GeoShapeJvm.S4J_CONTEXT.getShapeFactory.multiLineString()
    for (lineGs <- gs.lines) {
      builder.add( LineStringGsJvm.toSpatialShapeBuilder( lineGs ) )
    }
    builder.build()
  }

}
