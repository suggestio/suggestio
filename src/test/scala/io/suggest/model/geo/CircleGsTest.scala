package io.suggest.model.geo

import org.scalatest._
import org.scalatest.Matchers._
import org.elasticsearch.common.unit.DistanceUnit
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:22
 * Description: Тесты для CircleGs.
 */
class CircleGsTest extends FlatSpec with CoordRnd {

  /**
   * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_circle]]
   */
  "play.json DATA_FORMAT" should "parse data from documented ES circle example" in {
    val jsonStr =
      """
        |{
        |  "type" : "circle",
        |  "coordinates" : [-45.0, 45.0],
        |  "radius" : "100m"
        |}
      """.stripMargin
    val jsv = Json.parse(jsonStr)
    val res = jsv.validate(CircleGs.DATA_FORMAT)
    assert( res.isSuccess, res )
    val cgs = res.get
    cgs shouldBe CircleGs(
      center = GeoPoint(lon = -45.0, lat = 45.0),
      radius = Distance(100, DistanceUnit.METERS)
    )
  }

}
