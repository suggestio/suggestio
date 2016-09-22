package io.suggest.stat.m

import io.suggest.model.geo.GeoPoint
import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 11:08
  * Description: Тесты для модели [[MLocation]].
  */
class MLocationSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MLocation

  "JSON" should "support minimal model" in {
    jsonTest( MLocation() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MLocation(
        geo = MGeoLocData(
          coords = Some(GeoPoint(11, 22))
        ),
        geoIp = MGeoLocData(
          coords  = Some(GeoPoint(44, 55)),
          town    = Some("Xata")
        )
      )
    }
  }

}
