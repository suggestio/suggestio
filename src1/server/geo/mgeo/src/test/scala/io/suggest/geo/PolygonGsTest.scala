package io.suggest.geo

import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:37
 * Description: Тест для полигонов, которые используются для описания площадей узлов.
 */
class PolygonGsTest extends FlatSpec with CoordLineRnd {

  /**
   * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_ulink_url_http_www_geojson_org_geojson_spec_html_id4_polygon_ulink]]
   */
  "play-json DATA_FORMAT" should "parse no-holes example" in {
    val jsStr =
      """
        |{
        |  "type" : "polygon",
        |  "coordinates" : [
             [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
        |  ]
        |}
      """.stripMargin
    val jsr = Json.parse(jsStr)
      .validate( PolygonGs.DATA_FORMAT )
    assert(jsr.isSuccess, jsr)
    jsr.get shouldBe PolygonGs(
      LineStringGs(Seq(
        MGeoPoint(0.0, lon = 100.0),
        MGeoPoint(0.0, lon = 101.0),
        MGeoPoint(1.0, lon = 101.0),
        MGeoPoint(1.0, lon = 100.0),
        MGeoPoint(0.0, lon = 100.0)
      ))
    )
  }

  it should "parse 1-hole example" in {
    val jsStr =
      """
        |{
        |  "type" : "polygon",
        |  "coordinates" : [
        |     [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],
        |     [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2] ]
        |  ]
        |}
      """.stripMargin
    val jsr = Json.parse(jsStr)
      .validate( PolygonGs.DATA_FORMAT )
    assert( jsr.isSuccess, jsr )
    jsr.get shouldBe PolygonGs(
      outer = LineStringGs(Seq(
        MGeoPoint(0.0, lon = 100.0),
        MGeoPoint(0.0, lon = 101.0),
        MGeoPoint(1.0, lon = 101.0),
        MGeoPoint(1.0, lon = 100.0),
        MGeoPoint(0.0, lon = 100.0)
      )),
      holes = List(
        LineStringGs(Seq(
          MGeoPoint(0.2, lon = 100.2),
          MGeoPoint(0.2, lon = 100.8),
          MGeoPoint(0.8, lon = 100.8),
          MGeoPoint(0.8, lon = 100.2),
          MGeoPoint(0.2, lon = 100.2)
        ))
      )
    )
  }

}