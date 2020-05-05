package io.suggest.stat.m

import io.suggest.geo.MGeoPoint
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 11:08
  * Description: Тесты для модели [[MStatLocation]].
  */
class MStatLocationSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MStatLocation

  "JSON" should "support minimal model" in {
    jsonTest( MStatLocation() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MStatLocation(
        geo = MGeoLocData(
          coords = Some(MGeoPoint(lat = 11, lon = 22))
        ),
        geoIp = MGeoLocData(
          coords  = Some(MGeoPoint(lat = 44, lon = 55)),
          town    = Some("Xata")
        )
      )
    }
  }

}
