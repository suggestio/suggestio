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
class LineStringGsTest extends MultiPoingGeoShapeTest {
  override type T = LineStringGs
  override def companion = LineStringGs
}


/** Общий код multipoint-фигур лежит здесь. */
trait MultiPoingGeoShapeTest extends FlatSpec with Matchers with CoordLineRnd {

  protected val testsPerTry = 100
  override val minCoordLineLen = 2
  override val coordLineLenRnd = 40
  
  type T <: GeoShape

  protected def testName = companion.apply(Nil).getClass.getSimpleName
  
  def companion: MultiPointShapeStatic { type Shape_t = T }

  protected def mkTests(f: T => Unit): Unit = {
    (0 to testsPerTry) foreach { i =>
      val coords = rndCoordRow
      f(companion.apply(coords))
    }
  }


  testName should "serialize/deserialize to/from ES JSON" in {
    mkTests { lsgs =>
      val jsonStr = Json.stringify( lsgs.toPlayJson )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      companion.deserialize(jacksonJson)  shouldBe  Some(lsgs)
      lsgs.toEsShapeBuilder  should not be  null
    }
  }

}

