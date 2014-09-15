package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import play.api.libs.json.Json
import java.{util => ju}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:45
 * Description: Тесты для геошейпа MultiLineStringGs.
 */
class MultiLineStringGsTest extends FlatSpec with Matchers with CoordLineRnd {

  protected val testsPerTry = 10
  protected val lineMaxCount = 20

  protected def mkTests(f: MultiLineStringGs => Unit): Unit = {
    (0 to testsPerTry) foreach { i =>
      val coords = (0 to (rnd.nextInt(lineMaxCount) + 1)) map { j =>
        LineStringGs( rndCoordRow )
      }
      f(MultiLineStringGs(coords))
    }
  }


  "MultiLineStringGs" should "serialize/deserialize to/from ES JSON" in {
    mkTests { mlsgs =>
      val jsonStr = Json.stringify( mlsgs.toPlayJson() )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      MultiLineStringGs.deserialize(jacksonJson)  shouldBe  Some(mlsgs)
      mlsgs.toEsShapeBuilder  should not be  null
    }
  }

}
