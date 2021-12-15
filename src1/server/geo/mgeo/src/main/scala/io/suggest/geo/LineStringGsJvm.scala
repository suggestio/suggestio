package io.suggest.geo

import au.id.jazzy.play.geojson.{LineString, LngLat}
import io.suggest.common.geom.coord.GeoCoord_t
import org.elasticsearch.geometry.{Line => EsLine, LinearRing => EsLinearRing}
import org.locationtech.spatial4j.shape.Shape

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

  private def _toXysPair(gs: LineStringGs): (Array[Double], Array[Double]) = {
    def _toCoords(f: MGeoPoint => GeoCoord_t): Array[Double] = {
      gs.coords
        .map(f andThen (_.doubleValue))
        .toArray
    }

    (
      _toCoords( _.lon ),
      _toCoords( _.lat )
    )
  }

  override def toEsShapeBuilder(gs: LineStringGs): EsLine = {
    val (xs, ys) = _toXysPair( gs )
    new EsLine(xs, ys)
  }

  def toEsLinearRing(gs: LineStringGs): EsLinearRing = {
    val (xs, ys) = _toXysPair( gs )
    new EsLinearRing( xs, ys )
  }

  def toSpatialShapeBuilder(gs: LineStringGs) = {
    val builder = GeoShapeJvm.S4J_CONTEXT.getShapeFactory.lineString()

    for (pt <- gs.coords)
      builder.pointXY( pt.lon.doubleValue, pt.lat.doubleValue )

    builder
  }

  /** Convert to spatial4j shape. */
  override def toSpatialShape(gs: LineStringGs): Shape = {
    toSpatialShapeBuilder(gs)
      .build()
  }

}

