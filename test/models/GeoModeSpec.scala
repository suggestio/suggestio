package models

import functional.OneAppPerSuiteNoGlobalStart
import io.suggest.geo.MGeoPoint
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.14 10:56
 * Description: тесты для geo mode
 */
class GeoModeSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  // lazy, т.к. GeoPoint() дергает package, который дёргает другие модели, которые дёргают config.
  // И тогда вылетает ExceptionInInitializerError: There is no started application.
  private val gp0 = MGeoPoint(59.926185700000005, 30.2333629)
  private def gp1 = MGeoPoint(59.92626006720579, 30.233811233220834)


  "GeoMode" must {

    "parse empty stuff" in {
      GeoMode(None)  mustBe  GeoNone
      GeoMode(Some(""))  mustBe  GeoNone
    }

    "parse and bind qs ip value" in {
      GeoMode(Some("ip"))  mustBe  theSameInstanceAs(GeoIp)
      GeoMode(Some("IP"))  mustBe  theSameInstanceAs(GeoIp)
    }

    "parse and bind qs 'lat,lon' value" in {
      GeoMode(Some("59.926185700000005,30.2333629"))    mustBe  GeoLocation(gp0)
      GeoMode(Some("-66.245,30.2333629"))               mustBe  GeoLocation(MGeoPoint(-66.245, 30.2333629))
    }

    "parse stranger 'lat,lon' value" in {
      GeoMode(Some("-66.,30"))    mustBe  GeoLocation(MGeoPoint(-66.0, 30.0))
      GeoMode(Some("0,-0"))       mustBe  GeoLocation(MGeoPoint(0.0, 0.0))  // северный полюс.
    }

    // Для сбора статистики нужна инфа о точности.
    "parse and bind qs 'lat,lon,accur' value" in {
      GeoMode(Some("59.926185700000005,30.2333629,23.444444"))  mustBe  GeoLocation(gp0, Some(23.444444))
      GeoMode(Some("59.926185700000005,30.2333629,0"))          mustBe  GeoLocation(gp0, Some(0.0))
      GeoMode(Some("59.926185700000005,30.2333629,11"))         mustBe  GeoLocation(gp0, Some(11.0))
    }

    "handle invalid accuracy" in {
      GeoMode(Some("59.926185700000005,30.2333629,null"))       mustBe  GeoLocation(gp0)
      GeoMode(Some("59.926185700000005,30.2333629,undefined"))  mustBe  GeoLocation(gp0)
    }

    "handle fully missing accuracy" in {
      GeoMode(Some("59.926185700000005,30.2333629"))            mustBe GeoLocation(gp0)
      GeoMode(Some("59.92626006720579,30.233811233220834"))     mustBe GeoLocation(gp1)
    }

    "handle empty accuracy" in {
      GeoMode(Some("59.926185700000005,30.2333629,"))           mustBe  GeoLocation(gp0)
    }

  }

}
