package io.suggest.model.geo

import org.elasticsearch.common.geo.builders.{LineStringBuilder, ShapeBuilder}
import play.api.libs.json.JsArray
import scala.collection.JavaConversions._
import java.{lang => jl}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:08
 * Description: Линия на карте, которая состоит из двух и более точек.
 * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_geojson_org_geojson_spec_html_id3_linestring_ulink]]
 */
object LineStringGs extends MultiPointShapeStatic {

  override type Shape_t = LineStringGs

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


case class LineStringGs(coords: Seq[GeoPoint]) extends MultiPointShape {

  override def shapeType = GsTypes.linestring

  override type Shape_t = LineStringBuilder

  override protected def shapeBuilder: Shape_t = {
    ShapeBuilder.newLineString()
  }
}
