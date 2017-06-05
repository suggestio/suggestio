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
object MultiPolygonGs extends GsStaticJvmQuerable {

  override type Shape_t = MultiPolygonGs

  override def DATA_FORMAT: Format[MultiPolygonGs] = {
    (__ \ COORDS_ESFN).format[Seq[List[Seq[MGeoPoint]]]]
      .inmap [MultiPolygonGs] (
        {polyGss =>
          MultiPolygonGs(
            polyGss.map { polyCoords =>
              PolygonGs(polyCoords)
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
          PolygonGs.toPlayGeoJsonGeom( pgs ).coordinates
        }
        .toStream
    )
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val coords = for (pgs <- gs.polygons) yield {
      PolygonGs._toPlayJsonCoords( pgs )
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
        PolygonGs._renderToEsPolyBuilder(poly, polyBuilder)
        polyBuilder.close()
    }
  }

}


case class MultiPolygonGs(polygons: Seq[PolygonGs]) extends IGeoShapeQuerable {

  override def shapeType = GsTypes.MultiPolygon

  override def firstPoint = polygons.head.firstPoint

}

