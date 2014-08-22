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
class PolygonGsTest extends FlatSpec with Matchers with CoordRnd {

  protected val testsPerTry = 100
  
  protected def rndCoordRow: Seq[GeoPoint] = {
    val len = rnd.nextInt(200) + 50
    (0 to len).map { j =>
      GeoPoint(lat = newLat, lon = newLon)
    }
  }
  
  protected def mkTestsNoHoles(f: PolygonGs => Unit): Unit = {
    (0 to testsPerTry) foreach { i =>
      val coords = rndCoordRow
      PolygonGs(coords)
    }
  }

  
  "PolygonGs" should "serialize/deserialize to/from ES JSON" in {
    mkTestsNoHoles { pgs =>
      val jsonStr = Json.stringify( pgs.toPlayJson )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      PolygonGs.deserialize(jacksonJson)  shouldBe  Some(pgs)
      pgs.toEsShapeBuilder  should not be  null
    }
  }
  
}
