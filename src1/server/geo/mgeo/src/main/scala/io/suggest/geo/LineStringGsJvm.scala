package io.suggest.geo

import org.elasticsearch.common.geo.builders.LineStringBuilder
import au.id.jazzy.play.geojson.{LineString, LngLat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:08
 * Description: Линия на карте, которая состоит из двух и более точек.
 * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_geojson_org_geojson_spec_html_id3_linestring_ulink]]
 */
object LineStringGsJvm extends MultiPointShapeStatic {

  override type Shape_t = LineStringGs

  override def toPlayGeoJsonGeom(gs: Shape_t): LineString[LngLat] = {
    LineString(
      coordinates = LineStringGsJvm.coords2latLngs( gs.coords )
    )
  }

  override def toEsShapeBuilder(gs: LineStringGs) =
    new LineStringBuilder( gsCoords2esCoords(gs) )

}

