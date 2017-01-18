package io.suggest.model.geo

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json._

import io.suggest.geo.MGeoPoint

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 14:46
 * Description: Тесты для модели геоточки.
 */
class GeoPointTest extends FlatSpec with LatLonRnd[MGeoPoint] with PlayJsonTestUtil {

  import GeoPoint.Implicits._

  override type T = MGeoPoint

  override protected def mkInstance = MGeoPoint(lat = newLat, lon = newLon)


  "JSON (play)" should "simply support model" in {
    jsonTest( mkInstance )
  }

  private def _jsonTest(jsv: JsValue, result: T): Unit = {
    val res = jsv.validate[MGeoPoint]
    assert(res.isSuccess, res)
    res.get shouldBe result
  }
  private def _strJsonTest(jsonStr: String, result: T): Unit = {
    _jsonTest(Json.parse(jsonStr), result)
  }

  it should "deserialize from GeoJSON array: [lon, lat]" in {
    _strJsonTest(
      "[-34.4365346, 22.45365365]",
      MGeoPoint(lon = -34.4365346, lat = 22.45365365)
    )
  }

  it should """deserialize from String representation: "41.12,-71.34" """ in {
    _jsonTest(
      JsString("41.12,-71.34"),
      MGeoPoint(lat = 41.12, lon = -71.34)
    )
  }

}
