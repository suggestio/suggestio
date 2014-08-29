package io.suggest.model.geo

import GeoShape.COORDS_ESFN
import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.json.JsArray
import scala.collection.JavaConversions._
import java.{util => ju, lang => jl}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:08
 * Description: Линия на карте, которая состоит из двух и более точек.
 * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_geojson_org_geojson_spec_html_id3_linestring_ulink]]
 */
object LineStringGs {

  def deserialize(jmap: ju.Map[_,_]): Option[LineStringGs] = {
    Option(jmap get COORDS_ESFN)
      .map { rawCoords => LineStringGs( parseCoords(rawCoords) ) }
  }


  def parseCoords: PartialFunction[Any, Seq[GeoPoint]] = {
    case tr: TraversableOnce[_] =>
      tr.flatMap { gpRaw => GeoPoint.deserializeOpt(gpRaw) }
        .toSeq
    case l: jl.Iterable[_] =>
      parseCoords(l.iterator().toIterator)
  }

  def coords2playJson(coords: Seq[GeoPoint]): JsArray = {
    val coordsJson = coords map { _.toPlayGeoJson }
    JsArray(coordsJson)
  }

}


import LineStringGs._


case class LineStringGs(coords: Seq[GeoPoint]) extends GeoShape {

  override def shapeType = GsTypes.linestring

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal: FieldsJsonAcc = {
    val coordsJson = coords2playJson(coords)
    List(COORDS_ESFN -> coordsJson)
  }

  /** Отрендерить в изменяемый LineString ShapeBuilder для построения ES-запросов.
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  override def toEsShapeBuilder = {
    coords.foldLeft(ShapeBuilder.newLineString()) {
      (acc, gp)  =>  acc.point(gp.lon, gp.lat)
    }
  }
}
