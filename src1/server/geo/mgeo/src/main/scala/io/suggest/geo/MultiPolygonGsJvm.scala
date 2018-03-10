package io.suggest.geo

import org.elasticsearch.common.geo.builders.{MultiPolygonBuilder, ShapeBuilders}
import au.id.jazzy.play.geojson.{LngLat, MultiPolygon}

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
        .toStream
    )
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder(gs: Shape_t): MultiPolygonBuilder = {
    gs.polygons.foldLeft(ShapeBuilders.newMultiPolygon()) {
      (mpb, poly) =>
        val polyBuilder = PolygonGsJvm.toEsShapeBuilder( poly )
        mpb.polygon( polyBuilder )
    }
  }

}
