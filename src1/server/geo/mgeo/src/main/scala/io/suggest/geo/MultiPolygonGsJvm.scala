package io.suggest.geo

import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiPolygonBuilder, ShapeBuilder}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, MultiPolygon}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:58
 * Description: Мультиполигон - это список полигонов.
 */
object MultiPolygonGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = MultiPolygonGs

  override def DATA_FORMAT: Format[Shape_t] = {
    (__ \ COORDS_ESFN).format[Seq[List[Seq[MGeoPoint]]]]
      .inmap [Shape_t] (
        {polyGss =>
          MultiPolygonGs(
            for (polyCoords <- polyGss) yield {
              PolygonGsJvm(polyCoords)
            }
          )
        },
        {mpGs =>
          mpGs.polygons
            .map { _.toMpGss }
        }
      )
  }

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

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val coords = for (pgs <- gs.polygons) yield {
      PolygonGsJvm._toPlayJsonCoords( pgs )
    }
    val coordsArr = JsArray( coords )
    val row = COORDS_ESFN -> coordsArr
    row :: Nil
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder(gs: Shape_t): MultiPolygonBuilder = {
    gs.polygons.foldLeft(ShapeBuilder.newMultiPolygon) {
      (mpb, poly) =>
        val polyBuilder = mpb.polygon()
        PolygonGsJvm._renderToEsPolyBuilder(poly, polyBuilder)
        polyBuilder.close()
    }
  }

}
