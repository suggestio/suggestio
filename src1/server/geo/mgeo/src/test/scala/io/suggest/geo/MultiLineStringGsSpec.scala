package io.suggest.geo

import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.Json
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:45
 * Description: Тесты для геошейпа MultiLineStringGs.
 */
class MultiLineStringGsSpec extends FlatSpec with CoordLineRnd {

  /**
   * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_www_geojson_org_geojson_spec_html_id6_multilinestring_ulink]]
   */
  "play-json DATA_FORMAT" should "parse example data from documentation" in {
    val jsstr =
      """
        |{
        |  "type" : "multilinestring",
        |  "coordinates" : [
        |    [ [102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0] ],
        |    [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0] ],
        |    [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8] ]
        |  ]
        |}
      """.stripMargin
    val jsr = Json.parse(jsstr)
      .validate( MultiLineStringGs.DATA_FORMAT )
    assert( jsr.isSuccess, jsr )
    val mlsGs = jsr.get
    mlsGs.lines.size  shouldBe  3
    mlsGs.lines.head.firstPoint shouldBe MGeoPoint(lat = 2.0, lon = 102.0)
    for (line <- mlsGs.lines) {
      line.coords.size shouldBe 4
    }
    mlsGs.lines.head.coords.last shouldBe MGeoPoint(lat = 3.0, lon = 102.0)
    mlsGs.lines.last.coords.last shouldBe MGeoPoint(lat = 0.8, lon = 100.2)
  }

}
