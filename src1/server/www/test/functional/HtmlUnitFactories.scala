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


/** Имитация IE11. */
trait HtmlUnitIE11Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.INTERNET_EXPLORER
}

/** Имитация Firefox 24 ESR. */
trait HtmlUnitFF24Factory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.FIREFOX_38
}


/** Имитация более-менее свежего хрома. */
trait HtmlUnitChromeFactory extends HtmlUnitCustomBrowserFactory {
  override def huBrowserVersion = BrowserVersion.CHROME
}
