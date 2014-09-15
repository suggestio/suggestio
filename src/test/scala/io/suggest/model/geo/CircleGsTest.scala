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


  "CircleGs" should "serialize/deserialize to/from ES JSON" in {
    mkTests { cgs =>
      val jsonStr = Json.stringify( cgs.toPlayJson() )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      CircleGs.deserialize(jacksonJson)  shouldBe  Some(cgs)
      val esShape = cgs.toEsShapeBuilder
      esShape  should not equal null
    }
  }

}
