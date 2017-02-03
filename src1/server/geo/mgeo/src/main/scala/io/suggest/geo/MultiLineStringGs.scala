package io.suggest.geo

import io.suggest.geo.GeoShape.COORDS_ESFN
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiLineStringBuilder, ShapeBuilder}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LatLng, MultiLineString}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:32
 * Description: Представление multiline-объектов в рамках s.io.
 */

object MultiLineStringGs extends GsStatic {

  override type Shape_t = MultiLineStringGs

  override def DATA_FORMAT: Format[MultiLineStringGs] = {
    (__ \ COORDS_ESFN).format[Seq[Seq[MGeoPoint]]]
      .inmap [MultiLineStringGs] (
        { ss => apply( ss.map(LineStringGs.apply)) },
        { _.lines.map(_.coords) }
      )
  }

}


case class MultiLineStringGs(lines: Seq[LineStringGs]) extends GeoShapeQuerable {

  /** Используемый тип фигуры. */
  override def shapeType = GsTypes.multilinestring

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val playJson = lines.map { line => LineStringGs.coords2playJson(line.coords) }
    List(COORDS_ESFN -> JsArray(playJson))
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder: MultiLineStringBuilder = {
    // Заливаем линии
    lines.foldLeft(ShapeBuilder.newMultiLinestring) {
      (acc0, coordLine) =>
        // Заливаем все точки в линию
        coordLine.coords.foldLeft(acc0.linestring) {
          (acc1, e)  =>  acc1.point(e.lon, e.lat)
        }.end()
    }
  }

  override def firstPoint = lines.head.firstPoint

  override def toPlayGeoJsonGeom: MultiLineString[LatLng] = {
    MultiLineString(
      coordinates = lines.iterator.map(_.toPlayGeoJsonGeom.coordinates).toStream
    )
  }

}
