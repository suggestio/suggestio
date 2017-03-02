package market.showcase

import controllers.routes
import models.msc.{MScApiVsns, SiteQsArgs}
import org.scalatestplus.play._
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 14:54
 * Description: Тесты для showcase.
 */
trait GeoShowcaseSpecT extends PlaySpec with OneServerPerSuite with OneBrowserPerSuite {

  protected def marketGeoSiteUrl: Call = {
    val args = SiteQsArgs(apiVsn = MScApiVsns.Sjs1)
    routes.Sc.geoSite(x = args)
  }

  override implicit lazy val app: Application = {
    GuiceApplicationBuilder()
      .in( Mode.Test )
      .configure(
        "sio.https.disabled"          -> true,
        "sio.hostport.dflt"           -> s"localhost:$port"
      )
      .build()
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

// TODO Тесты для реальных браузеров:
//class GeoShowcaseSpec extends GeoShowcaseSpecT with ChromeFactory
//class GeoShowcaseSpecFF extends GeoShowcaseSpecT with FirefoxFactory // TODO Починить. Что-то астральное.

// HtmlUnit-тесты:
//class GeoShowcaseSpecHuIE8 extends GeoShowcaseSpecT with HtmlUnitIE8Factory
//class GeoShowcaseSpecHuIE9 extends GeoShowcaseSpecT with HtmlUnitIE9Factory
//class GeoShowcaseSpecHuIE11 extends GeoShowcaseSpecT with HtmlUnitIE11Factory
//class GeoShowcaseSpecHuFF17 extends GeoShowcaseSpecT with HtmlUnitFF17Factory
//class GeoShowcaseSpecHuFF24 extends GeoShowcaseSpecT with HtmlUnitFF24Factory
//class GeoShowcaseSpecHuChrome extends GeoShowcaseSpecT with HtmlUnitChromeFactory
