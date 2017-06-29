package io.suggest.geo

import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiLineStringBuilder, ShapeBuilders}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, MultiLineString}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:32
 * Description: Представление multiline-объектов в рамках s.io.
 */

object MultiLineStringGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = MultiLineStringGs

  override def DATA_FORMAT: Format[MultiLineStringGs] = {
    (__ \ COORDS_ESFN).format[Seq[Seq[MGeoPoint]]]
      .inmap [MultiLineStringGs] (
        { ss => MultiLineStringGs( ss.map(LineStringGs.apply)) },
        { _.lines.map(_.coords) }
      )
  }

  override def toPlayGeoJsonGeom(mlsGs: Shape_t): MultiLineString[LngLat] = {
    MultiLineString(
      coordinates = mlsGs.lines
        .iterator
        .map { lsGs =>
          LineStringGsJvm.toPlayGeoJsonGeom( lsGs ).coordinates
        }
        .toStream
    )
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val playJson = for (line <- gs.lines) yield {
      LineStringGsJvm.coords2playJson( line.coords )
    }
    List(COORDS_ESFN -> JsArray(playJson))
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
