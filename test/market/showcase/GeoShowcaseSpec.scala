package market.showcase

import controllers.routes
import functional._
import org.scalatestplus.play._
import play.api.test.FakeApplication

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 14:54
 * Description: Тесты для showcase.
 */
trait GeoShowcaseSpecT extends PlaySpec with OneServerPerSuite with OneBrowserPerSuite {

  val marketGeoSiteUrl = routes.MarketShowcase.geoSite()

  override implicit lazy val app: FakeApplication = {
    new FakeApplication(
      additionalConfiguration = Map(
        "persona.audience.url"        -> s"http://localhost:$port",
        "sio.proto.dflt"              -> "http",
        "sio.hostport.dflt"           -> s"localhost:$port",
        "radius.server.tiny.enabled"  -> false,
        "cats.install.mart.allowed"   -> false
      )
    )
  }

  "market/geo/site" must {
    "provide web content" in {
      go to s"http://localhost:$port${marketGeoSiteUrl.url}"
      pageTitle.toLowerCase must include("suggest.io")
      eventually {
        click on find(id("smGeoScreenButton")).value
      }
      eventually {
        click on find(id("smGeoLocationLabel")).value
      }
      eventually {
        click on find(id("smGeoScreenCloseButton")).value
      }
      click on find(id("smNavigationLayerButton")).value
      click on find(id("smShopsTab")).value
    }
  }
}

// Тесты для реальных браузеров:
class GeoShowcaseSpec extends GeoShowcaseSpecT with ChromeFactory
//class GeoShowcaseSpecFF extends GeoShowcaseSpecT with FirefoxFactory // TODO Починить. Что-то астральное.

// HtmlUnit-тесты:
class GeoShowcaseSpecHuIe8 extends GeoShowcaseSpecT with HtmlUnitIE8Factory
class GeoShowcaseSpecHuIe9 extends GeoShowcaseSpecT with HtmlUnitIE9Factory
class GeoShowcaseSpecHuIe11 extends GeoShowcaseSpecT with HtmlUnitIE11Factory
class GeoShowcaseSpecHuFF17 extends GeoShowcaseSpecT with HtmlUnitFF17Factory
class GeoShowcaseSpecHuFF24 extends GeoShowcaseSpecT with HtmlUnitFF24Factory
class GeoShowcaseSpecHuChrome extends GeoShowcaseSpecT with HtmlUnitChromeFactory

