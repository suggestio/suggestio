package io.suggest.geo

import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShape.COORDS_ESFN
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
object MultiPolygonGs extends GsStaticJvm {

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

}


case class MultiPolygonGs(polygons: Seq[PolygonGs]) extends GeoShapeQuerable {

  override def shapeType = GsTypes.MultiPolygon

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val coords = JsArray(polygons.map { _._toPlayJsonCoords })
    List(COORDS_ESFN -> coords)
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder: MultiPolygonBuilder = {
    polygons.foldLeft(ShapeBuilder.newMultiPolygon) {
      (mpb, poly) =>
        val polyBuilder = mpb.polygon()
        poly.renderToEsPolyBuilder(polyBuilder)
        polyBuilder.close()
    }
  }

  override def firstPoint = polygons.head.firstPoint

  override def toPlayGeoJsonGeom: MultiPolygon[LngLat] = {
    MultiPolygon(
      coordinates = polygons
        .iterator
        .map { _.toPlayGeoJsonGeom.coordinates }
        .toStream
    )
  }

}

