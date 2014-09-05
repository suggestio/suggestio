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


  "GeoMode" must {

    "parse empty stuff" in {
      GeoMode(None)  mustBe  GeoNone
      GeoMode(Some(""))  mustBe  GeoNone
    }

    "parse and bind qs ip value" in {
      GeoMode(Some("ip"))  mustBe  theSameInstanceAs(GeoIp)
    }

    "parse and bind qs lat,lon value" in {
      GeoMode(Some("59.926185700000005,30.2333629"))   mustBe  GeoLocation(59.926185700000005, 30.2333629)
      GeoMode(Some("-66.245,30.2333629"))   mustBe  GeoLocation(-66.245, 30.2333629)
    }

  }

}
