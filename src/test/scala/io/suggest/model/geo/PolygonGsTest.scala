package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import play.api.libs.json.Json
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:37
 * Description: Тест для полигонов, которые используются для описания площадей узлов.
 */
class PolygonGsTest extends FlatSpec with Matchers with CoordLineRnd {

  protected val testsPerTry = 100

  protected def mkTestsNoHoles[T](f: PolygonGs => T): Unit = {
    for (i <- 0 to testsPerTry) {
      val coords = rndCoordRow
      f(PolygonGs(LineStringGs(coords)))
    }
  }

  
  "PolygonGs" should "serialize/deserialize to/from ES JSON" in {
    mkTestsNoHoles { pgs =>
      val jsonStr = Json.stringify( pgs.toPlayJson() )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      PolygonGs.deserialize(jacksonJson)  shouldBe  Some(pgs)
      pgs.toEsShapeBuilder  should not be  null
    }
  }

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
        GeoPoint(0.0, lon = 100.0),
        GeoPoint(0.0, lon = 101.0),
        GeoPoint(1.0, lon = 101.0),
        GeoPoint(1.0, lon = 100.0),
        GeoPoint(0.0, lon = 100.0)
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
        GeoPoint(0.0, lon = 100.0),
        GeoPoint(0.0, lon = 101.0),
        GeoPoint(1.0, lon = 101.0),
        GeoPoint(1.0, lon = 100.0),
        GeoPoint(0.0, lon = 100.0)
      )),
      holes = List(
        LineStringGs(Seq(
          GeoPoint(0.2, lon = 100.2),
          GeoPoint(0.2, lon = 100.8),
          GeoPoint(0.8, lon = 100.8),
          GeoPoint(0.8, lon = 100.2),
          GeoPoint(0.2, lon = 100.2)
        ))
      )
    )
  }

}
