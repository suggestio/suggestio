package models

import org.scalatestplus.play._
import play.api.GlobalSettings
import play.api.test.FakeApplication

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.14 10:56
 * Description: тесты для geo mode
 */
class GeoModeSpec extends PlaySpec with OneAppPerSuite {

/** Штатный Global производит долгую инициализацию, которая нам не нужен.
    * Нужен только доступ к конфигу. Ускоряем запуск: */
  override implicit lazy val app = FakeApplication(
    withGlobal = Some(new GlobalSettings() {
      override def onStart(app: play.api.Application) {
        super.onStart(app)
        println("Started dummy fake application, without Global.onStart() initialization.")
      }
    })
  )

  // lazy, т.к. GeoPoint() дергает package, который дёргает другие модели, которые дёргают config.
  // И тогда вылетает ExceptionInInitializerError: There is no started application.
  private lazy val gp0 = GeoPoint(59.926185700000005, 30.2333629)


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
      GeoMode(Some("59.926185700000005,30.2333629"))   mustBe  GeoLocation(gp0)
      GeoMode(Some("-66.245,30.2333629"))   mustBe  GeoLocation(GeoPoint(-66.245, 30.2333629))
    }

    "parse stranger 'lat,lon' value" in {
      GeoMode(Some("-66.,30"))    mustBe  GeoLocation(GeoPoint(-66.0, 30.0))
      GeoMode(Some("0,-0"))       mustBe  GeoLocation(GeoPoint(0.0, 0.0))  // северный полюс.
    }

    // Для сбора статистики нужна инфа о точности.
    "parse and bind qs 'lat,lon,accur' value" in {
      GeoMode(Some("59.926185700000005,30.2333629,23.444444"))  mustBe  GeoLocation(gp0, Some(23.444444))
      GeoMode(Some("59.926185700000005,30.2333629,0"))          mustBe  GeoLocation(gp0, Some(0.0))
      GeoMode(Some("59.926185700000005,30.2333629,11"))         mustBe  GeoLocation(gp0, Some(11.0))
    }

    "parse and bind qs 'lat,lon,accur' with invalid/missing accuracy" in {
      GeoMode(Some("59.926185700000005,30.2333629,null"))       mustBe  GeoLocation(gp0)
      GeoMode(Some("59.926185700000005,30.2333629,"))           mustBe  GeoLocation(gp0)
    }

  }

}
