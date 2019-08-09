package io.suggest.geo

import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:22
 * Description: Тесты для CircleGs.
 */
class CircleGsSpec extends FlatSpec with CoordRnd {

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
    val res = jsv.validate( IGeoShape.JsonFormats.allStoragesEsFormatter.circle )
    assert( res.isSuccess, res )
    val cgs = res.get
    cgs shouldBe CircleGs(
      center  = MGeoPoint(lon = -45.0, lat = 45.0),
      radiusM = 100
    )
  }

}
