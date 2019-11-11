package io.suggest.geo

import org.elasticsearch.common.geo.builders.{MultiLineStringBuilder, ShapeBuilders}
import au.id.jazzy.play.geojson.{LngLat, MultiLineString}


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
  override def toEsShapeBuilder(gs: Shape_t): MultiLineStringBuilder = {
    // Заливаем линии
    gs.lines.foldLeft(ShapeBuilders.newMultiLinestring()) {
      (acc0, coordLine) =>
        // Заливаем все точки в линию
        val lsb = LineStringGsJvm.toEsShapeBuilder(coordLine)
        acc0.linestring(lsb)
    }
  }

}
