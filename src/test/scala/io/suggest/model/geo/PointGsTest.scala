package io.suggest.model.geo

import org.scalatest._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:10
 * Description: Тесты для модели точки, заданной через индексируемые spatial4j-поля.
 */
class PointGsTest extends FlatSpec with Matchers with LatLonRnd[PointGs] {

  override protected def mkInstance = PointGs(GeoPoint(lat = newLat, lon = newLon))


  /**
   * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_geojson_org_geojson_spec_html_id2_point_ulink]]
   */
  "play.json DATA_FORMAT" should "parse data from documented ES example" in {
    val jsonStr =
      """
        |{
        |  "type" : "point",
        |  "coordinates" : [-77.03653, 38.897676]
        |}
      """.stripMargin
    val jsr = Json.parse(jsonStr).validate(PointGs.DATA_FORMAT)
    assert(jsr.isSuccess, jsr)
    val pgs = jsr.get
    pgs shouldBe PointGs(GeoPoint(lon = -77.03653, lat = 38.897676))
  }

}
