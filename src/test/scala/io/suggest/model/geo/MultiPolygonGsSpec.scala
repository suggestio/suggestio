package io.suggest.model.geo

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 11:35
 * Description:
 */
class MultiPolygonGsSpec extends FlatSpec {

  /**
   * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_www_geojson_org_geojson_spec_html_id7_multipolygon_ulink]]
   */
  "play-json DATA_FORMAT" should "parse example from documentation" in {
    val jsStr =
      """
        |{
        |  "type" : "multipolygon",
        |  "coordinates" : [
        |    [ [[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]] ],
        |    [ [[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]],
        |      [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]] ]
        |  ]
        |}
      """.stripMargin
    val jsr = Json.parse(jsStr)
      .validate( MultiPolygonGs.DATA_FORMAT )
    assert( jsr.isSuccess,  jsr )
    val gs = jsr.get
    gs.polygons.size shouldBe 2
    assert( gs.polygons.head.holes.isEmpty, gs.polygons.head )
    gs.polygons.last.holes.size shouldBe 1
  }

}
