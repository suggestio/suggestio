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

  protected def mkTestsNoHoles(f: PolygonGs => Unit): Unit = {
    (0 to testsPerTry) foreach { i =>
      val coords = rndCoordRow
      f(PolygonGs(coords))
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
