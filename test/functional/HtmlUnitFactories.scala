package functional

import com.gargoylesoftware.htmlunit.BrowserVersion
import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.scalatestplus.play.BrowserFactory.UnavailableDriver
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.10.14 17:00
 * Description:
 */

trait HtmlUnitCustomBrowserFactory extends BrowserFactory {
  def huJavascriptEnabled = true

  def huBrowserVersion: BrowserVersion

  override def createWebDriver(): WebDriver = {
    try {
      val htmlUnitDriver = new HtmlUnitDriver(huBrowserVersion)
      htmlUnitDriver.setJavascriptEnabled(huJavascriptEnabled)
      htmlUnitDriver

    } catch {
      case ex: Throwable => UnavailableDriver(Some(ex), "cantCreateHtmlUnitDriver")
    }
  }
}

/** Имитация IE8. */
trait HtmlUnitIE8Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.INTERNET_EXPLORER_8
}

/** Имитация IE9. */
trait HtmlUnitIE9Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.INTERNET_EXPLORER_9
}

/** Имитация IE11. */
trait HtmlUnitIE11Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.INTERNET_EXPLORER_11
}

/** Имитация Firefox 24 ESR. */
trait HtmlUnitFF24Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.FIREFOX_24
}

/** Имитация Firefox 17 ESR. */
trait HtmlUnitFF17Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.FIREFOX_17
}

/** Имитация более-менее свежего хрома. */
trait HtmlUnitChromeFactory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.CHROME
}
