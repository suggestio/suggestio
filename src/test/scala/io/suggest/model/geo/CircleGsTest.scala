package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import org.elasticsearch.common.unit.DistanceUnit
import play.api.libs.json.Json
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:22
 * Description: Тесты для CircleGs.
 */
class CircleGsTest extends FlatSpec with Matchers with CoordRnd {

  protected def distanceValRnd = rnd.nextDouble() * 500

  protected val tryPerUnit = 100

  protected def mkTests(f: CircleGs => Unit): Unit = {
    DistanceUnit.values().foreach { du =>
      (0 to tryPerUnit).foreach { i =>
        val distance = Distance(distanceValRnd, du)
        val gp = GeoPoint(lat = newLat, lon = newLon)
        val cgs = CircleGs(gp, distance)
        f(cgs)
      }
    }
  }


  "Jackson JSON" should "serialize/deserialize to/from ES JSON" in {
    mkTests { cgs =>
      val jsonStr = Json.stringify( cgs.toPlayJson() )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      CircleGs.deserialize(jacksonJson)  shouldBe  Some(cgs)
      val esShape = cgs.toEsShapeBuilder
      esShape  should not equal null
    }
  }


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
