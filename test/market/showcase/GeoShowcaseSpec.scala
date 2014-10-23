package market.showcase

import controllers.routes
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
        find(id("smGeoLocationLabel")).value.toString()
      }
      click on find(id("smGeoScreenCloseButton")).value
      click on find(id("smNavigationLayerButton")).value
      click on find(id("smShopsTab")).value
    }
  }
}

class GeoShowcaseSpec extends GeoShowcaseSpecT with ChromeFactory
class GeoShowcaseSpecHU extends GeoShowcaseSpecT with HtmlUnitFactory
//class GeoShowcaseSpecFF extends GeoShowcaseSpecT with FirefoxFactory // TODO scalatest виснет, запустив firefox.
