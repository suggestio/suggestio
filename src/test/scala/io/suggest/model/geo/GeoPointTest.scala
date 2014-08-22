package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import play.api.libs.json._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 14:46
 * Description: Тесты для модели геоточки.
 */
class GeoPointTest extends FlatSpec with Matchers with LatLonRnd[GeoPoint] {

  "GeoPoint model" should "serialize/deserialize to/from JSON using GeoJSON format" in {
    mkTests { gp =>
      val jsonStr = Json.stringify( gp.toPlayGeoJson )
      val jacksonJson = JacksonWrapper.deserialize [ju.ArrayList[Any]] (jsonStr)
      GeoPoint.deserializeOpt( jacksonJson )  shouldBe  Some(gp)
    }
  }

  it should "serialize/deserialize to/from JSON using string lat,lon representation" in {
    mkTests { gp =>
      val jsonStr = Json.stringify( gp.toPlayJsonEsStr )
      val jacksonJson = JacksonWrapper.deserialize [String] (jsonStr)
      GeoPoint.deserializeOpt( jacksonJson )  shouldBe  Some(gp)
    }
  }

  it should "serialize/deserialize to/from JSON using JSON Object format" in {
    mkTests { gp =>
      val jsonStr = Json.stringify( gp.toPlayJson )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      GeoPoint.deserializeOpt( jacksonJson )  shouldBe  Some(gp)
    }
  }

  override protected def mkInstance = GeoPoint(lat = newLat, lon = newLon)
}
