package io.suggest.geo

import org.elasticsearch.common.geo.builders.{LineStringBuilder, ShapeBuilder}
import play.api.libs.json.JsArray
import play.extras.geojson.{LineString, LngLat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:08
 * Description: Линия на карте, которая состоит из двух и более точек.
 * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_geojson_org_geojson_spec_html_id3_linestring_ulink]]
 */
object LineStringGs extends MultiPointShapeStatic {

  override type Shape_t = LineStringGs

  def coords2playJson(coords: Seq[MGeoPoint]): JsArray = {
    val coordsJson = coords.map( GeoPoint.toPlayGeoJson )
    JsArray(coordsJson)
  }

  override def toPlayGeoJsonGeom(gs: Shape_t): LineString[LngLat] = {
    LineString(
      coordinates = LineStringGs.coords2latLngs( gs.coords )
    )
  }

  override type ShapeBuilder_t = LineStringBuilder

  override protected def shapeBuilder: ShapeBuilder_t = {
    ShapeBuilder.newLineString()
  }

}


case class LineStringGs(coords: Seq[MGeoPoint]) extends MultiPointShape {

  override def shapeType = GsTypes.LineString

  override def firstPoint = coords.head

}
