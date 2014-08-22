package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import play.api.libs.json.Json
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:10
 * Description: Тесты для модели точки, заданной через индексируемые spatial4j-поля.
 */
class PointGsTest extends FlatSpec with Matchers with LatLonRnd[PointGs] {

  "PoingGs" should "serialize/deserialize to/from JSON" in {
    mkTests { pgs =>
      val jsonStr = Json.stringify( pgs.toPlayJson )
      // TODO Надо тестить через XContentHelpers
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      PointGs.deserialize(jacksonJson)  shouldBe  Some(pgs)
      pgs.toEsShapeBuilder  should not equal null
    }
  }

  override protected def mkInstance = PointGs(GeoPoint(lat = newLat, lon = newLon))
}
