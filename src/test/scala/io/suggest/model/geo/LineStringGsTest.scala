package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import play.api.libs.json.Json
import java.{util => ju}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:23
 * Description: Тесты для Linestring GeoShape'ов.
 */
class LineStringGsTest extends FlatSpec with Matchers with CoordLineRnd {

  protected val testsPerTry = 100
  override val minCoordLineLen = 2
  override val coordLineLenRnd = 40

  protected def mkTests(f: LineStringGs => Unit): Unit = {
    (0 to testsPerTry) foreach { i =>
      val coords = rndCoordRow
      f(LineStringGs(coords))
    }
  }


  "LineStringGs" should "serialize/deserialize to/from ES JSON" in {
    mkTests { lsgs =>
      val jsonStr = Json.stringify( lsgs.toPlayJson )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      LineStringGs.deserialize(jacksonJson)  shouldBe  Some(lsgs)
      lsgs.toEsShapeBuilder  should not be  null
    }
  }

}
